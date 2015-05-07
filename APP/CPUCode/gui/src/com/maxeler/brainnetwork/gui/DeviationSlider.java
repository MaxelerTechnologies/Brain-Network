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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//Slider to control the deviation parameter

public class DeviationSlider extends JSlider implements ChangeListener{

	//UID generated for component serialization
	private static final long serialVersionUID = 6741899577090973938L;

	//Reference to image correlation kernel and speed-up
	private LinearCorrelation [] kernel = null;
	private SpeedUpTextField speedup = null;

	//Constructor
	public DeviationSlider(LinearCorrelation [] kernel, SpeedUpTextField speedup){

		//Instantiate a vertical slider with range [4.0,6.5]
		super(JSlider.VERTICAL,40,65,45);

		//Store external references
		this.kernel=kernel;
		this.speedup = speedup;

		//Slider size
		setSize(80,225);

		//Draw slider scale
		setMinorTickSpacing(1);
		setMajorTickSpacing(5);
		setPaintTicks(true);
		setPaintTrack(true);

		//Activate snap
		setSnapToTicks(true);

		//Custom scale labels
		Dictionary<Integer,JComponent> scale = new Hashtable<Integer, JComponent>();
		scale.put(40,new JLabel("4.0"));
		scale.put(45,new JLabel("4.5"));
		scale.put(50,new JLabel("5.0"));
		scale.put(55,new JLabel("5.5"));
		scale.put(60,new JLabel("6.0"));
		scale.put(65,new JLabel("6.5"));
		setLabelTable(scale);
		setPaintLabels(true);

		//Initialize deviation threshold
		for (int i=0; i<kernel.length; ++i)
			kernel[i].point_threshold=getFloatValue();

		//In order to react to action on the slider
		addChangeListener(this);
	}

	//Get slider value as float in range [4.0,6.5] with accuracy 0.1
	public float getFloatValue(){

		float temp = getValue();
		return temp/10;
	}

	//Describe reaction
	@Override
	public void stateChanged(ChangeEvent e){

		//Modify correlation threshold for each kernel
		for (int i=0; i<kernel.length; ++i)
			kernel[i].point_threshold=getFloatValue();

		BrainNetwork.restart = true;
		speedup.resetToZero();
	}

}