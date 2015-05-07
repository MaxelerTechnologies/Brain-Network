/*********************************************************************
 * Maxeler Technologies: BrainNetwork                                *
 *                                                                   *
 * Version: 1.2                                                      *
 * Date:    05 July 2013                                             *
 *                                                                   *
 * GUI code source file                                              *
 *                                                                   *
 *********************************************************************/

package com.maxeler.brainnetwork.gui;

import javax.swing.JTextField;

public class TimeTextField extends JTextField{

	//UID generated for component serialization
	private static final long serialVersionUID = -4759370119898888403L;

	//Time to represent
	double time = 0;

	//Reference to speed-up field
	private SpeedUpTextField speed_up =null;
	private boolean numerator = false;

	//Constructor
	public TimeTextField(SpeedUpTextField speed_up, boolean numerator){

		//Instantiate a text field
		super("0.000000s");

		//Get reference to speed up field
		this.speed_up = speed_up;

		//Specify how to calculate the speed up
		this.numerator = numerator;

		//Text size
		setSize(80,18);

		//Right alignment
		setHorizontalAlignment(JTextField.RIGHT);

		//Disable editing
	    setEditable(false);
	}

	//Change time
	public void setTime(double time){

		//Update private variable
		this.time = time;

		//Update text field
		setText(String.format("%.6fs",time));

		//Update speed up field
		if (numerator)
			speed_up.setNumerator(time);
		else
			speed_up.setDenominator(time);

	}

}
