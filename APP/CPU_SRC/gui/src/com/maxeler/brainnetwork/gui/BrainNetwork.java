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

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

//GUI for brain network application

public class BrainNetwork {

	//Buffer to contain the sequence of brain images
	private static BufferedImage [] images = null;

	//Current frame (suppose 10 fps)
	public static int frame = 99;

	//Control linear correlation
	public static boolean running = false;

	//Control daemon restarting when parameter are changed
	public static boolean restart = false;

	//Get images from zip file in which are stored
	private static void loadImagesFromZip(String zip_name){

		//Temporary list
		LinkedList<BufferedImage> image_list = new LinkedList<BufferedImage>();

		try{

			//Stream to unzip compressed data
			FileInputStream fis = new FileInputStream(zip_name);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));

			//Read one by one the images stored in the zip
			ZipEntry entry = zis.getNextEntry();
			while( entry != null) {

				//Get current image and save into list
				BufferedImage current_image = ImageIO.read(zis);
				image_list.add(current_image);

				//Go to the next image
				entry = zis.getNextEntry();
			}
			zis.close();

		//Manage exception
		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		//Copy list into the array
		images = new BufferedImage[image_list.size()];
		for (int i=0; i<image_list.size(); ++i)
			images[i] = image_list.get(i);

	}


	//Reference to application files
	protected static String image_file = null;
	protected static String daemon_file = null;
	protected static String library_file = null;

	public static void main(String[] args) {

		//Get file references
		image_file = args[0];
		daemon_file = args[1];
		library_file = args[2];

		//Load images from file
		loadImagesFromZip(image_file);

		//Create the application window
		JFrame f = new JFrame("Brain Network");
		f.setSize(880, 350);
		f.setResizable(false);
		f.setLocation(0,0);
		f.getContentPane().setLayout(null);

		//Create two loops of brain images
		ImageLoop [] im = new ImageLoop[2];
		im[0] = new ImageLoop(images);
		im[0].setSize(348,260);
		im[0].setLocation(0,4);
		im[0].setToolTipText("Dynamic linear correlation analysis on brain images can potentially discover the underlying network of neural interactions.");
		f.getContentPane().add(im[0]);
		im[1] = new ImageLoop(images);
		im[1].setSize(348,260);
		im[1].setLocation(352,4);
		im[1].setToolTipText("Dynamic linear correlation analysis on brain images can potentially discover the underlying network of neural interactions.");
		f.getContentPane().add(im[1]);

		//Text field to show the speed up
		SpeedUpTextField speed_up = new SpeedUpTextField();
		speed_up.setLocation(615,298);
		f.getContentPane().add(speed_up);

		//Two text fields to show the running time
		TimeTextField [] time = new TimeTextField[2];
		time[0] = new TimeTextField(speed_up,true);
		time[0].setLocation(265,273);
		f.getContentPane().add(time[0]);
		time[1] = new TimeTextField(speed_up,false);
		time[1].setLocation(615,273);
		f.getContentPane().add(time[1]);

		//Two linear correlation kernels, one for CPU and one for DFE
		LinearCorrelation [] l = new LinearCorrelation [2];
		l[0] = new LinearCorrelation(im[0],time[0],true);
		l[1] = new LinearCorrelation(im[1],time[1],false);
		f.addWindowListener(l[0]);
		f.addWindowListener(l[1]);

		//Slide to control the deviation threshold
		DeviationSlider deviation_slider = new DeviationSlider(l,speed_up);
		deviation_slider.setLocation(710,10);
		deviation_slider.setToolTipText("Standard deviation threshold related with fluorescence. Decrease the value for analyzing more points.");
		f.getContentPane().add(deviation_slider);
		JLabel deviation_label = new JLabel("Deviation");
		deviation_label.setLocation(710,245);
		deviation_label.setSize(100,15);
		f.getContentPane().add(deviation_label);

		//Slide to control the correlation threshold
		CorrelationSlider correlation_slider = new CorrelationSlider(l,speed_up);
		correlation_slider.setLocation(790,10);
		correlation_slider.setToolTipText("Linear correlation threshold related with interactions. Decrease the value for drawing more edges.");
		f.getContentPane().add(correlation_slider);
		JLabel correlation_label = new JLabel("Correlation");
		correlation_label.setLocation(790,245);
		correlation_label.setSize(100,15);
		f.getContentPane().add(correlation_label);

		//Button to start/stop computation
		ComputeButton compute = new ComputeButton(l,speed_up);
		compute.setLocation(710,275);
		f.getContentPane().add(compute);

		//Various labels
		JLabel cpu_label = new JLabel("CPU Analysis");
		cpu_label.setLocation(10,275);
		cpu_label.setSize(150,15);
		cpu_label.setFont(new Font("Courier New", Font.BOLD, 16));
		f.getContentPane().add(cpu_label);
		JLabel dfe_label = new JLabel("DFE Analysis");
		dfe_label.setLocation(360,275);
		dfe_label.setSize(150,15);
		dfe_label.setFont(new Font("Courier New", Font.BOLD, 16));
		f.getContentPane().add(dfe_label);
		JLabel cpu_time_label = new JLabel("CPU Time");
		cpu_time_label.setLocation(200,275);
		cpu_time_label.setSize(100,15);
		f.getContentPane().add(cpu_time_label);
		JLabel dfe_time_label = new JLabel("DFE Time");
		dfe_time_label.setLocation(545,275);
		dfe_time_label.setSize(100,15);
		f.getContentPane().add(dfe_time_label);
		JLabel speed_up_label = new JLabel("Speedup");
		speed_up_label.setLocation(545,300);
		speed_up_label.setSize(100,15);
		f.getContentPane().add(speed_up_label);

		//Show GUI
		f.setVisible(true);

		//Timer to control brain images (10 fps) and computation trigger
		ComputationTrigger ct = new ComputationTrigger(im,l);
		Timer t = new Timer(100,ct);
		t.start();

	}
}
