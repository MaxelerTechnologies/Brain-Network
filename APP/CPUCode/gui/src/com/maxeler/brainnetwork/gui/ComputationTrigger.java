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

public class ComputationTrigger implements ActionListener{

	//Modulo 10 counter
	private int counter = 0;

	//Reference to images and linear correlation kernel
	private ImageLoop [] loops = null;
	private LinearCorrelation [] kernel = null;

	//Constructor simply gets references to external component
	public ComputationTrigger(ImageLoop [] loops,LinearCorrelation [] kernel){

		//Linear correlation kernel
		this.kernel = kernel;

		//Image loops to update... with non-blocking mutex
		this.loops = loops;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		//Increment frame with wrapping
		++BrainNetwork.frame;
		if (BrainNetwork.frame==loops[0].images.length)
			BrainNetwork.frame=0;

		//When count zero, trigger computation as independent thread
		if (counter==0  && BrainNetwork.running){

			if (BrainNetwork.restart)
				for (int i=0; i<kernel.length; ++i)
					kernel[i].restart();
			BrainNetwork.restart = false;

			for (int i=0; i<kernel.length; ++i){
				Thread t = new Thread(kernel[i]);
				t.start();
			}
		}

		//Trigger update images
		for (int i=0; i<loops.length; ++i){
			Thread t = new Thread(loops[i]);
			t.start();
		}

		//Increment counter with wrapping
		++counter;
		if (counter==10)
			counter=0;

	}

}
