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

public class SpeedUpTextField extends JTextField{

	//UID generated for component serialization
	private static final long serialVersionUID = 8425222564625929169L;

	//Variable to calculate speed-up
	private double numerator = 1;
	private double denominator = 1;

	//Reset variable
	private boolean numerator_reset = true;
	private boolean denominator_reset = true;

	//Constructor
	public SpeedUpTextField (){

		//Instantiate a text field
		super("0.000x");

		//Text size
		setSize(80,18);

		//Right alignment
		setHorizontalAlignment(JTextField.RIGHT);

		//Disable editing
		setEditable(false);

	}

	//Change numerator
	public void setNumerator(double time){

		//Update private variable

		if (numerator_reset){
			numerator = time;
			numerator_reset = false;
		}

		//When both have been update
		if (!numerator_reset && !denominator_reset){

			//Reset variable
			numerator_reset = true;
			denominator_reset = true;

			//Update text field
			setText(String.format("%.3fx",numerator/denominator));
		}
	}

	//Change denominator
	public void setDenominator(double time){

		//Update private variable
		if (denominator_reset){
			denominator_reset = false;
			denominator = time;
		}

		//When both have been update
		if (!numerator_reset && !denominator_reset){

			//Reset variable
			numerator_reset = true;
			denominator_reset = true;

			//Update text field
			setText(String.format("%.3fx",numerator/denominator));
		}
	}

	//Reset speed-up to zero
	public void resetToZero(){

		//Update private variables
		numerator = 1;
		denominator = 1;

		//Reset variable
		numerator_reset = true;
		denominator_reset = true;

		//Update text field
		setText(String.format("0.000x"));
	}

}
