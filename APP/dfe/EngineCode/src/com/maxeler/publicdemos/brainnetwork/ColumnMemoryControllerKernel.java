/*********************************************************************
 * Maxeler Technologies: BrainNetwork                                *
 *                                                                   *
 * Version: 1.2                                                      *
 * Date:    05 July 2013                                             *
 *                                                                   *
 * DFE code source file                                              *
 *                                                                   *
 *********************************************************************/

package com.maxeler.publicdemos.brainnetwork;

import com.maxeler.maxcompiler.v2.kernelcompiler.Kernel;
import com.maxeler.maxcompiler.v2.kernelcompiler.KernelParameters;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.KernelMath;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.LMemCommandStream;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.core.Count;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.core.Count.Counter;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.base.DFEType;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.base.DFEVar;
import com.maxeler.maxcompiler.v2.utils.MathUtils;

// Kernel for generating the right sequence of memory commands for reading column bursts
// The goal is to follow the "triangular shape" of the all-pairs quadratic computation

public class ColumnMemoryControllerKernel extends Kernel {

	public ColumnMemoryControllerKernel(KernelParameters parameters, BrainNetworkParams params, int burst_size_byte) {
		super(parameters);

		//Store time series length, pipelines and burst size
		this.window = params.getWindow();
		this.num_pipes = params.getNumPipes();
		this.burst_size_byte = burst_size_byte;

		//Get the number of points as scalar... 28 bits corresponds to 268 MPixel images
		DFEVar n_active_points = io.scalarInput("n_active_points",controlType);

		//Control logic for generating memory fetch signal
		DFEVar memory_fetching = generateControl(n_active_points);

		//Calculate the address of the starting burst
		DFEVar starting_burst = getStartingAddress();

		//Calculate how many burst to transfer
		DFEVar burst_to_transfer = getBurstToTransfer();

		//Stream out the command to prefetch memory (only when appropriate)
		LMemCommandStream.makeKernelOutput("column_bursts_from_DRAM_cmd",memory_fetching,
				starting_burst.cast(dfeUInt(32)), 		//Address in bursts where to start read/write
				burst_to_transfer.cast(dfeUInt(8)),		//Number of bursts to read/write (at least 1)
				constant.var(dfeUInt(8),1),				//Offset/stride(2^9) where to skip after read/write (at least 1)
				constant.var(dfeUInt(4),0),				//Select current stream in the command generator group
				constant.var(dfeBool(),0)				//In order to raise an interrupt
				);
	}

	//Time series length, pipelines and burst size
	private int window = 0;
	private int num_pipes = 0;
	private int burst_size_byte = 0;

	//Type used for counters
	private final DFEType controlType = dfeUInt(28);

	//Points contained in one burst
	private int data_per_burst = 0;

	//Addresses(indexes) to iterate over the points
	private DFEVar inner_address = null;

	//Position of the first burst after the point array
	private DFEVar first_burst_outside_memory = null;

	//Generate the control logic to fetch memory
	private DFEVar generateControl(DFEVar n_active_points){

		//Calculate the position of the burst right after the last point in memory (rounding to the next multiple of n_active_points)
		data_per_burst = burst_size_byte/(4+3+3+window);
		DFEVar n_active_points_offset = KernelMath.modulo(n_active_points,data_per_burst);
		first_burst_outside_memory = (n_active_points_offset>0) ?
				n_active_points + data_per_burst - n_active_points_offset.cast(controlType) :
				n_active_points;

		//Signal to control the iteration of inner loop (they decrease since we have a triangular computation)
		DFEVar inner_loop_max = controlType.newInstance(this);

		//Counter for the inner loop j (decreasing number of iterations using a feedback signal to vary wrapping point)
		Count.Params innerCounterParams = control.count.makeParams(28)
			.withMax(inner_loop_max);
		Counter innerCounter = control.count.makeCounter(innerCounterParams);
		DFEVar inner_count = innerCounter.getCount();

		//Counter for the outer loop i (row increment depends by the number of pipes k)
		Count.Params outerCounterParams = control.count.makeParams(28)
			.withEnable(innerCounter.getWrap())
			.withInc(num_pipes);
		Counter outerCounter = control.count.makeCounter(outerCounterParams);
		DFEVar outer_address = outerCounter.getCount();

		//Feedback to control the inner loop (wrap when j=n-i-1)... -1 offset is used to have zero delay in the feedback loop.
		//This does not affect correct looping since inner loop uses multiple iteration (>1), thus j=n-i-1 is available at the right time
		DFEVar loop_control = (n_active_points.cast(controlType)-1)-outer_address;
		DFEVar loop_control_feedback = stream.offset(loop_control,-1);
		inner_loop_max <== loop_control_feedback;

		//Calculate the current position in the inner loop (i+1 is starting point, counter is a moving offset)
		inner_address = inner_count+outer_address+1;

		//We send command for the first element
		DFEVar first_element = outer_address.eq(0) & inner_address.eq(1);

		//At each new row, considering 1 clock cycle after the wrap signal is active
		DFEVar is_new_row = stream.offset(innerCounter.getWrap(),-1);

		// More in general, we keep a counter in order to detect the burst multiple of 128. This counter resets at each new row
		Count.Params multipleOfBlockCounterParams = control.count.makeParams(MathUtils.bitsToRepresent(128*data_per_burst))
			.withReset(is_new_row)
			.withMax(128*data_per_burst);
		Counter multipleOfBlockCounter = control.count.makeCounter(multipleOfBlockCounterParams);
		DFEVar multiple_of_block_count = multipleOfBlockCounter.getCount();

		//For each row, we have an initial offset in order to align the command with the first element in the block multiple
		DFEVar offset_to_align_counter = KernelMath.modulo(outer_address+1,data_per_burst);
		DFEVar aligned_counter =
			multipleOfBlockCounter.getCount()+offset_to_align_counter.cast(multiple_of_block_count.getType());

		//When sum of counter and offset is a 128*data_per_burst (block size)... we are on the first element of the block multiple,
		// thus we can send a command
		DFEVar is_counter_multiple = aligned_counter.eq(128*data_per_burst);

		//When offset is zero (row is already aligned) the previous condition cannot work (never 128*data_per_burst since
		// counter has max value 128*data_per_burst-1).
		is_counter_multiple |= multiple_of_block_count.eq(0) & (aligned_counter.eq(0));

		//We stop sending memory commands when row is composed by just one burst
		DFEVar row_with_multiple_bursts = ((outer_address+1) < (first_burst_outside_memory-data_per_burst));

		//Compose the memory fetching signal
		DFEVar memory_fetching = first_element |
			((is_counter_multiple | is_new_row ) & row_with_multiple_bursts);

		return memory_fetching;
	}

	//Calculate the address of the starting burst
	private DFEVar getStartingAddress(){

		return KernelMath.divMod(inner_address,constant.var(data_per_burst)).getQuotient().cast(dfeUInt(27));
	}

	//Calculate how many burst to transfer
	private DFEVar getBurstToTransfer(){

		//Calculate how many burst to transfer (at least one, up to 128) from the distance between n and the element to fetch
		DFEVar aligned_inner_address = inner_address - KernelMath.modulo(inner_address, data_per_burst).cast(controlType);
		DFEVar element_to_transfer = first_burst_outside_memory - aligned_inner_address;

		//Round to upper integer and saturate to 128 (maximumm transfer size)
		DFEVar burst_to_transfer = KernelMath.divMod(element_to_transfer,constant.var(data_per_burst)).getQuotient();
		burst_to_transfer = (burst_to_transfer<=128) ? burst_to_transfer : 128;

		return burst_to_transfer;
	}

}
