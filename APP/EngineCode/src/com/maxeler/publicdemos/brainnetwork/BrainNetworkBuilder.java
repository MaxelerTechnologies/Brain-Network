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

import com.maxeler.maxcompiler.v2.managers.BuildConfig;
import com.maxeler.maxcompiler.v2.managers.BuildConfig.Effort;
import com.maxeler.maxcompiler.v2.managers.BuildConfig.Level;

public class BrainNetworkBuilder {

	public static void main(String[] args) {

		//Get the parameter from command line
		BrainNetworkParams param = new BrainNetworkParams(args);

		//Create the custom manager
		BrainNetworkManager m = new BrainNetworkManager(param);

		//Set the configuration for building the bitstream
		BuildConfig build = new BuildConfig(Level.FULL_BUILD);
		build.setBuildEffort(Effort.HIGH);
		build.setMPPRRetryNearMissesThreshold(1000);
		build.setMPPRParallelism(param.getMPPRThreads());
		build.setMPPRCostTableSearchRange(param.getMPPRStartCT(),param.getMPPREndCT());
		build.setEnableTimingAnalysis(true);
		m.setBuildConfig(build);

		//Set the frequency of the bitstream
		m.config.setDefaultStreamClockFrequency(param.getFrequency());

		//Store npipes in the max-file
		m.addMaxFileConstant("NPIPES", param.getNumPipes());
		m.addMaxFileConstant("FREQUENCY", param.getFrequency());

		//Build log printout
		param.logParameters(m);

		//Finally, build the bitstream
		m.build();
	}

}

