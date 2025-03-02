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

import java.util.LinkedList;

import com.maxeler.maxcompiler.v2.kernelcompiler.Kernel;
import com.maxeler.maxcompiler.v2.kernelcompiler.KernelParameters;
import com.maxeler.maxcompiler.v2.kernelcompiler.Optimization;
import com.maxeler.maxcompiler.v2.kernelcompiler.op_management.MathOps;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.KernelMath;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.Reductions;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.core.Count;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.core.Count.Counter;
import com.maxeler.maxcompiler.v2.kernelcompiler.stdlib.core.Count.WrapMode;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.base.DFEFix.SignMode;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.base.DFEType;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.base.DFEVar;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.composite.DFEStruct;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.composite.DFEStructType;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.composite.DFEVector;
import com.maxeler.maxcompiler.v2.kernelcompiler.types.composite.DFEVectorType;
import com.maxeler.maxcompiler.v2.utils.MathUtils;

// Multi-pipeline kernel that calculates linear correlation between all point pairs using DRAM

public class LinearCorrelationKernel extends Kernel {

	public LinearCorrelationKernel(KernelParameters parameters, BrainNetworkParams params, int burst_size_byte) {
		super(parameters);

		//Store time series length, pipelines and burst size
		window = params.getWindow();
		num_pipes = params.getNumPipes();
		this.burst_size_byte = burst_size_byte;

		//Get the number of points as scalar... (up to 256M points)
		DFEVar n_active_points = io.scalarInput("n_active_points",dfeUInt(28));

		//Generate control logic to loop over the set of points
		generateControl(n_active_points);

		//Get column burst from DRAM when appropriate and extract current point
		DFEStruct column_burst_with_padding = io.input("column_bursts_from_DRAM",burstWithPaddingType,column_burst_fetching);
		DFEStruct column_point = getColumnPoint(column_burst_with_padding, n_active_points);

		//Get row burst from DRAM when appropriate and extract k points to feed pipelines
		DFEStruct row_burst_with_padding = io.input("row_bursts_from_DRAM",burstWithPaddingType,row_burst_fetching);
		DFEStruct [] row_points = getRowPoints(row_burst_with_padding,n_active_points);

		//Use pipelines in order to obtain correlation between points (edges)
		DFEVector<DFEStruct> correlation_edges  = correlate(column_point, row_points);

		//Compare correlation with threshold and tag active edges
		DFEVar correlation_threshold = io.scalarInput("correlation_threshold",dfeFloat(8,24));
		DFEVector<DFEVar> edge_tags  = selectEdges(correlation_edges, correlation_threshold);

		//Stream correlation edges
		io.output("correlation_edges", correlation_edges, multipleCorrelationEdgeType, ~stop_computing);

		//Stream edge tags
		io.output("control_signal", edge_tags, multipleEdgeTags, ~stop_computing);

		//Signal when loop ends
		io.output("stop_computing", stop_computing, dfeBool());
	}

	//Type for describing correlation edges
	DFEVectorType<DFEStruct> multipleCorrelationEdgeType = null;

	//Processing pipelines to calculate correlation between points
	private DFEVector<DFEStruct> correlate(DFEStruct column_point, DFEStruct [] row_buffer){

		//Extract time series from column point (used by all the pipelines)
		DFEVector<DFEVar> temporal_series_column = column_point["temporal_series"];

		//Covariance calculation for each pipeline(defined as (E[ab]-E[a]*E[b])/(std(a)*std(b)))
		DFEVar [] correlation = new DFEVar[num_pipes];
		//Set operation precision along the entire computation
		optimization.pushFixOpMode(Optimization.bitSizeLimit(32), Optimization.offsetNoOverflow(), MathOps.ALL);
		for (int i=0; i<num_pipes; ++i){

			//Get the temporal series on current pipe
			DFEVector<DFEVar> temporal_series_row = row_buffer[i]["temporal_series"];

			//Calculate E[ab] starting with summation...
			//Use DSPs to implement "multiply and add" (9bitsx9bits -> 1 DSP -> 30 DPSs)
			DFEVar average_product  = constant.var(0);
			for (int j=0; j<window; ++j){
				average_product += temporal_series_row.get(j).cast(dfeInt(9))* temporal_series_column.get(j).cast(dfeInt(9));
			}

			//Obtain E[ab] dividing by window... implemented as product of inverse in order to use DSPs (18x20 on single DSP)
			average_product *= constant.var(dfeFixMax(18, 1.0/window, SignMode.TWOSCOMPLEMENT), 1.0/window);

			//Calculate product between average E[a]*E[b] (24x24 still on DSP)
			DFEVar product_between_average = ((DFEVar)row_buffer[i]["average"]) * ((DFEVar)column_point["average"]);

			//Calculate covariance as E[ab]-E[a]*E[b]
			DFEVar covariance = average_product - product_between_average;

			//Calculate product between standard deviations std(a)*std(b)... mapped on DSPs (24x24 on two DSPs)
			DFEVar product_between_deviations = (((DFEVar)row_buffer[i]["standard_deviation"]) * ((DFEVar)column_point["standard_deviation"]));

			//Reduce resources used for division by squash optimization
			//optimization.pushSquashFactor(0.5);
			//optimization.popSquashFactor();

			//Finally, calculate the correlation (range [-1,1] rounded with 30 fractional bits)
			optimization.pushFixOpMode(Optimization.bitSizeExact(32), Optimization.offsetExact(-30), MathOps.DIV);
			correlation[i] = covariance/product_between_deviations;
			optimization.popFixOpMode(MathOps.DIV);

		}
		optimization.popFixOpMode(MathOps.ALL);

		//Define the structure of correlation edges
		DFEStructType correlationEdgeType = new DFEStructType(
				new DFEStructType.StructFieldType("pixel_a",dfeUInt(32)),
				new DFEStructType.StructFieldType("pixel_b",dfeUInt(32)),
				new DFEStructType.StructFieldType("correlation",dfeFloat(8,24))
		);

		//Compose the correlation edges
		multipleCorrelationEdgeType = new DFEVectorType<DFEStruct>(correlationEdgeType,num_pipes);
		DFEVector<DFEStruct> correlation_edges = multipleCorrelationEdgeType.newInstance(this);
		for (int i=0; i<num_pipes; ++i){

			//Compose the current edge
			DFEStruct temp_correlation_edge = correlationEdgeType.newInstance(this);
			temp_correlation_edge.set("pixel_a",row_buffer[i]["pixel"]);
			temp_correlation_edge.set("pixel_b",column_point["pixel"]);
			temp_correlation_edge.set("correlation",correlation[i].cast(dfeFloat(8,24)));

			//Connect the pipe to the result
			correlation_edges[i] <== temp_correlation_edge;
		}

		return correlation_edges;
	}

	//Type for describing edge tags
	DFEVectorType<DFEVar> multipleEdgeTags = null;

	//Select edges over the given threshold
	private DFEVector<DFEVar> selectEdges(DFEVector<DFEStruct> correlation_edges, DFEVar correlation_threshold){

		//Define type for edge tags
		multipleEdgeTags = new DFEVectorType<DFEVar>(dfeBool(),num_pipes);
		DFEVector<DFEVar> edge_tags = multipleEdgeTags.newInstance(this);

		//For each pipeline
		for (int i=0; i<num_pipes; ++i){

			//Compare correlation and threshold
			DFEVar tag = (((DFEVar)(correlation_edges[i]["correlation"])) >= correlation_threshold)
				| (((DFEVar)(correlation_edges[i]["correlation"])) <= (-correlation_threshold));

			//During the preloading phase, marginal elements are calculated for free thus I may keep the edges
			//First pipe is always active
			if (i!=0)
				tag &= (pipeOffsetCounter.getCount()>=i);

			//Shut down the pipe during loading phase and during the last cycle
			tag &= ~stop_computing;

			//Connect the pipe to the result
			edge_tags[i] <== tag;
		}

		return edge_tags;
	}

	//Time series length, pipelines and burst size
	private int window = 0;
	private int num_pipes = 0;
	private int burst_size_byte = 0;

	//Points contained in one burst
	private int data_per_burst = 0;

	//Type used for counters
	private final DFEType controlType = dfeUInt(28);

	//Addresses(indexes) to iterate over the points
	private DFEVar outer_address = null;
	private DFEVar inner_address = null;

	//Signal when new row
	private DFEVar is_new_row = null;

	//Signal when loop end
	private DFEVar stop_computing = null;

	//Counter used in case of multiple pipelines for reading different row points in different clock cycles
	private Counter pipeOffsetCounter = null;

	//Model a burst of points taking account possible padding
	private DFEStructType burstWithPaddingType = null;

	//Generate the control logic to loop over the points
	private void generateControl(DFEVar n_active_points){

		//Signal to control the iteration of inner loop (they decrease since we have a triangular computation)
		DFEVar inner_loop_max = controlType.newInstance(this);

		//Counter for the inner loop j (as mentioned, decreasing number of iterations using a feedback signal)
		Count.Params innerCounterParams = control.count.makeParams(28)
			.withMax(inner_loop_max);
		Counter innerCounter = control.count.makeCounter(innerCounterParams);
		DFEVar inner_count = innerCounter.getCount();

		//Counter for the outer loop i (row increment depends by the number of pipes k)
		Count.Params outerCounterParams = control.count.makeParams(28)
			.withEnable(innerCounter.getWrap())
			.withInc(num_pipes);
		Counter outerCounter = control.count.makeCounter(outerCounterParams);
		outer_address = outerCounter.getCount();

		//Feedback to control the inner loop (wrap when j=n-i-1)... -1 offset to have zero delay in the feedback.
		//This does not affect functionality because inner loop uses multiple iteration, thus j=n-i-1 can get the right value before wrapping
		DFEVar loop_control = (n_active_points.cast(controlType)-1)-outer_address;
		DFEVar loop_control_feedback = stream.offset(loop_control,-1);
		inner_loop_max <== loop_control_feedback;

		//Calculate the current position in the inner loop (i+1 is starting point, counter is a moving offset)
		inner_address = inner_count+outer_address+1;

		//At each new row, considering 1 clock cycle after the wrap signal is active
		is_new_row = stream.offset(innerCounter.getWrap(),-1);

		//Active point type
		DFEStructType activePointType = new DFEStructType(
				new DFEStructType.StructFieldType("pixel",dfeUInt(32)),
				new DFEStructType.StructFieldType("average",dfeFixOffset(24,-16,SignMode.UNSIGNED)),
				new DFEStructType.StructFieldType("standard_deviation",dfeFixOffset(24,-16,SignMode.UNSIGNED)),
				new DFEStructType.StructFieldType("temporal_series",new DFEVectorType<DFEVar>(dfeUInt(8),window))
				);

		//Calculate possible padding within a burst
		data_per_burst = burst_size_byte / (4+3+3+window);
		int padding = burst_size_byte % (4+3+3+window);

		//Define the data structure for a burst, with optional padding
		DFEVectorType<DFEStruct> burstType = new DFEVectorType<DFEStruct>(activePointType,data_per_burst);
		if (padding>0){

			//With padding
			burstWithPaddingType = new DFEStructType(
				new DFEStructType.StructFieldType("data",burstType),
				new DFEStructType.StructFieldType("padding",dfeRawBits(8*padding))
			);
		}
		else {

			//With no padding
			burstWithPaddingType = new DFEStructType(
				new DFEStructType.StructFieldType("data",burstType)
			);
		}

		//In case of multiple pipes, it is necessary an additional counter to read multiple row in different clock cycles
		if (num_pipes>1){

			//Counter for the preloading, keep resetting at each outer iteration
			Count.Params pipeOffsetCounterParams = control.count.makeParams(MathUtils.bitsToAddress(num_pipes))
				.withReset(is_new_row)
				.withMax(num_pipes-1)
				.withWrapMode(WrapMode.STOP_AT_MAX);
			pipeOffsetCounter = control.count.makeCounter(pipeOffsetCounterParams);
		}

		//Recognize when loop end... needed for sending last memory command with interrupt
		DFEVar loop_end =  inner_address>=n_active_points;
		stop_computing = loop_end;
		Count.Params stopCounterParams = control.count.makeParams(1)
			.withEnable(loop_end)
			.withWrapMode(WrapMode.STOP_AT_MAX);
		Counter stopCounter = control.count.makeCounter(stopCounterParams);
		stop_computing |= stopCounter.getCount().cast(dfeBool());

	}

	//Signal for fetching the column
	private DFEVar column_burst_fetching = dfeBool().newInstance(this);

	//Get column point from DRAM
	private DFEStruct getColumnPoint(DFEStruct column_burst_with_padding, DFEVar n_active_points){

		//Column burst are fetched from a controlled input, assuming that a custom memory controller
		// provides the right sequence of input data

		//Control the column burst fetching according to the following conditions...

		//At the first element in the loop (condition (i,j)=(0,1))
		DFEVar first_column_fetching = (outer_address.eq(0) & inner_address.eq(1));

		//At each multiple of burst size, after calculating the remainder on j
		DFEVar column_burst_offset = KernelMath.modulo(inner_address,data_per_burst);
		DFEVar multiple_of_column_burst = column_burst_offset.eq(0);

		//When the current line just contains the last burst, we do not need to fetch because it is already buffered...
		// to detect the condition, we calculate the position of the first element in the last burst
		DFEVar last_multiple_of_burst = n_active_points -1 - KernelMath.modulo(n_active_points-1,data_per_burst).cast(controlType);
		DFEVar row_with_multiple_burst = (outer_address+1 < last_multiple_of_burst);

		//Compose the column fetching signal
		column_burst_fetching <== (first_column_fetching |
			((multiple_of_column_burst | is_new_row) & row_with_multiple_burst));

		//Use a multiplexer to select between the element in the burst
		DFEVector<DFEStruct> column_burst=column_burst_with_padding["data"];
		LinkedList<DFEStruct> inputs_list = new LinkedList<DFEStruct>();
		for (int i=0; i<data_per_burst; ++i){
			inputs_list.add(column_burst.get(i));
		}

		//Try optimize multiplexer latency
		optimization.pushPipeliningFactor(0);
		DFEStruct column_point = control.mux(column_burst_offset.cast(dfeUInt(MathUtils.bitsToAddress(data_per_burst))),inputs_list);
		optimization.popPipeliningFactor();

		return column_point;
	}

	//Signal for fetching the rows
	private DFEVar row_burst_fetching = dfeBool().newInstance(this);

	//Get multiple row points from DRAM (processed in parallel by the computing pipelines)
	private DFEStruct [] getRowPoints(DFEStruct row_burst_with_padding, DFEVar n_active_points){

		//Regarding row bursts, we simply use a linear address generator since we read just once the sequence of points

		//Calculate the row element to fetch taking account of the multipipe case
		DFEVar row_element = null;
		if (num_pipes>1) {
			row_element = outer_address + pipeOffsetCounter.getCount().cast(controlType);
		}
		else{
			row_element = outer_address;
		}

		//Given a row element, calculate its offset within a burst
		DFEVar row_burst_offset = KernelMath.modulo(row_element,data_per_burst);

		//The reading of row i is incremental, thus we have to activate row burst fetch signal at each multiple of burst
		DFEVar multiple_of_row_burst = row_burst_offset.eq(0);

		//Variable row_element_offset may keep its value along the inner loop, thus we have to activate the fetching only once.
		//Moreover we take of the extra cycle needed for the interrupt on the result DRAM stream
		DFEVar avoid_unnecessary_fetching = (inner_address <= outer_address+num_pipes) & (inner_address<n_active_points);

		//Compose the column fetching signal
		row_burst_fetching <== multiple_of_row_burst & avoid_unnecessary_fetching;

		//Use a multiplexer to select between the element in the burst
		DFEVector<DFEStruct> row_burst=row_burst_with_padding["data"];
		LinkedList<DFEStruct> inputs_list = new LinkedList<DFEStruct>();
		for (int i=0; i<data_per_burst; ++i){
			inputs_list.add(row_burst.get(i));
		}

		//Try optimize multiplexer latency
		optimization.pushPipeliningFactor(1);
		DFEStruct row_point = control.mux(row_burst_offset.cast(dfeUInt(MathUtils.bitsToAddress(data_per_burst))),inputs_list);
		optimization.popPipeliningFactor();

		//Multipipe loop mechanism is composed by multiple row elements maintained in buffers and a moving column element
		//We need just k-1 buffers (last element is directly read from DRAM)
		//Each buffer remains transparent up to a certain cycle in order to store the correspondent row element
		DFEVar [] row_buffer_control = new DFEVar[num_pipes-1];
		for (int i=0; i<num_pipes-1; ++i){
			row_buffer_control[i] = (pipeOffsetCounter.getCount()<=i);
		}

		//Connect the buffers to the row point
		DFEStruct [] row_buffer = new DFEStruct[num_pipes];
		row_buffer[num_pipes-1] = row_point;
		for (int i=0; i<num_pipes-1; ++i){
			row_buffer[i] = Reductions.streamHold(row_point,row_buffer_control[i]);
		}

		return row_buffer;
	}

}
