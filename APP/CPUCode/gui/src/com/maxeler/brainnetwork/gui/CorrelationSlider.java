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

//Slider to control the correlation parameter

public class CorrelationSlider extends JSlider implements ChangeListener{

	//UID generated for component serialization
	private static final long serialVersionUID = -6434934323282280112L;

	//Reference to image correlation kernel and speed-up
	private LinearCorrelation [] kernel = null;
	private SpeedUpTextField speedup = null;

	//Constructor
	public CorrelationSlider(LinearCorrelation [] kernel, SpeedUpTextField speedup){

		//Instantiate a vertical slider with range [0.6,8.5]
		super(JSlider.VERTICAL,60,85,78);

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
		scale.put(60,new JLabel(".60"));
		scale.put(65,new JLabel(".65"));
		scale.put(70,new JLabel(".70"));
		scale.put(75,new JLabel(".75"));
		scale.put(80,new JLabel(".80"));
		scale.put(85,new JLabel(".85"));
		setLabelTable(scale);
		setPaintLabels(true);

		//Modify deviation threshold for each kernel
		for (int i=0; i<kernel.length; ++i)
			kernel[i].edge_threshold=getFloatValue();

		//In order to react to action on the slider
		addChangeListener(this);
	}

	//Get slider value as float in range [0.60,0.85] with accuracy 0.01
	public float getFloatValue(){

		float temp = getValue();
		return temp/100;
	}

	//Describe reaction
	@Override
	public void stateChanged(ChangeEvent e){

		//Modify correlation threshold for each kernel
		for (int i=0; i<kernel.length; ++i)
			kernel[i].edge_threshold=getFloatValue();

		BrainNetwork.restart = true;
		speedup.resetToZero();
	}

}