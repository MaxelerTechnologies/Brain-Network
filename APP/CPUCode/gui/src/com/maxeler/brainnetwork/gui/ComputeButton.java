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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToggleButton;

//Toggle button used to start/stop the demo
public class ComputeButton extends JToggleButton implements ActionListener, Runnable{

	//UID generated for component serialization
	private static final long serialVersionUID = -145094948834407005L;

	//Reference to image correlation kernel and speed-up
	private LinearCorrelation [] kernel = null;
	private SpeedUpTextField speedup = null;

	//Constructor
	public ComputeButton (LinearCorrelation [] kernel, SpeedUpTextField speedup){

		//Instantiate a toggle button initialized to false
		super("Compute",false);

		//Store external references
		this.kernel = kernel;
		this.speedup = speedup;

		//Button size
		setSize(150,40);

		//In order to react to action on the button
		addActionListener(this);
	}

	//Describe reaction to event
	@Override
	public void actionPerformed(ActionEvent actionEvent) {

		//Trigger a new thread to manage the event
		Thread t = new Thread(this);
		t.start();
	}

	//Thread to manage the event
	@Override
	public void run() {

		//Deactivate the button until the threads end
		setEnabled(false);

		//Switch the running flag according to the status
		BrainNetwork.running = isSelected();

		//When stop execution, restart all the correlation kernel
		if (!BrainNetwork.running)
			for (int i=0; i<kernel.length; ++i){
				kernel[i].restart();
			}
		else
			speedup.resetToZero();

		//Finally, reactivate the button
		setEnabled(true);
	}

}
