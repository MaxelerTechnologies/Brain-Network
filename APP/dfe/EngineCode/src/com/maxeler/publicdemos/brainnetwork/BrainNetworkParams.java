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

import com.maxeler.maxcompiler.v2.build.EngineParameters;
import com.maxeler.maxcompiler.v2.managers.DFEModel;

/**
 * Parameters that control the hardware build.
 * <p><h4>Notes</h4>
 * <ul>
 * <li> Builds have been created successfully for 8 and 12 pipes on Maia and Coria. </li>
 * <li> If the "<code>window</code>" size is changed from the default 30, then
 *      the <code>BrainNetworkGUI</code> code will need to be modified accordingly.</li>
 * <li> The "<code>enableECC</code>" parameter must be set to <code>true</code> for Maia builds.</li>
 * </ul>
 */
public class BrainNetworkParams extends EngineParameters {

	//Constructor
	public BrainNetworkParams(String[] args) {
		super(args);
	}

	//Get methods for reading the various parameters
	public int getNumPipes() {
		return getParam("numPipes");
	}

	public int getFrequency() {
		return getParam("frequency");
	}

	public int getWindow() {
		return getParam("window");
	}

	public boolean isSimulation() {
		return getParam("target") == Target.DFE_SIM;
	}

	public boolean enableECC() {
		return getParam("enableECC");
	}

	public String getSuffix() {
		return getParam("suffix");
	}

	public int getCENumPartitions() {
		return getParam("ceNumPartitions");
	}

	//Implement abstract method to declare the various parame
	@Override
	protected void declarations() {
		declareParam("target",          Target.class,           Target.DFE);
		declareParam("DFEModel",        DataType.DFEMODEL,      DFEModel.VECTIS);
		declareParam("maxFileName",     DataType.STRING,        "BrainNetwork");
		declareParam("numPipes",        DataType.INT,           8);
		declareParam("frequency",       DataType.INT,           150);
		declareParam("window",          DataType.INT,           30);
		declareParam("simulate",        DataType.BOOL,          false);
		declareParam("enableECC",       DataType.BOOL,          false);
		declareParam("ceNumPartitions", DataType.INT,           3);
		declareParam("suffix",          DataType.STRING,        "");
	}

	//Implement abstract method
	@Override
	protected void deriveParameters() {

	}

	//Implement abstract method to construct the name of the build
	@Override
	public String getBuildName() {

		//Base prefix
		StringBuilder buildName = new StringBuilder(getMaxFileName());

		//Append the pipe number
		buildName.append("_p"+getNumPipes());

		buildName.append("_" + getTarget().toString());

		//Append the kernel frequency number
		buildName.append("_f"+getFrequency());

		//Append the CE partitions
		buildName.append("_ce"+getCENumPartitions());

		//Append the board name
		buildName.append("_" + getDFEModel().toString());

		//Append window size
		buildName.append("_w" + getWindow());

		//Append optional suffix
		String suffix = getSuffix();
		if(!suffix.equals("")) {
			buildName.append("_" + suffix);
		}

		return buildName.toString();
	}

	@Override
	protected void validate() { }

}