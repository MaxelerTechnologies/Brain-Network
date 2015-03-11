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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import javax.swing.JComponent;

//Describe the correlation between two points (pixel)
class Edge{

	//Two points
	public int [] point = new int [2];
	public float correlation;

	//Given x resolution, get selected endpoint as (x,y) coordinates
	public Point getEndPoint(int resolution, int select){

		return new Point (point[select%2]%resolution,point[select%2]/resolution);
	}
}

//Loop of images to emulate video data on which it is possible to draw correlation edges
public class ImageLoop extends JComponent implements Runnable{

	//UID generated for component serialization
	private static final long serialVersionUID = -4925797469985056177L;

	//Two list of edges, one used to draw superimposed edges and the other to add edges from outside
	private final LinkedList<Edge> edges = new LinkedList<Edge>();
	private final LinkedList<Edge> edges_temp = new LinkedList<Edge>();

	//Reference to image sequence
	protected BufferedImage [] images = null;

	//Constructor
	public ImageLoop(BufferedImage [] images) {

		//Copy reference
		this.images = images;
	}

	//Non-blocking synchronization... if one image update is too slow, skip the next frame
	Semaphore mutex = new Semaphore(1,true);

	//In order to run as image update as thread
	@Override
	public void run() {

		//Check the non-blocking mutex
		if (!mutex.tryAcquire()){
			return;
		}

		//Repaint component
		this.repaint();

		//Release the resource
		mutex.release();
	}

	//Mutexes to synchronize the addition and clearing of edges
	Semaphore edge_mutex = new Semaphore(1,true);
	Semaphore clean_mutex = new Semaphore(1,true);

	//Paint the component
	@Override
	public void paintComponent(Graphics g) {

		//Paint the background
		super.paintComponent(g);

		//Draw the brain image
		g.drawImage(images[BrainNetwork.frame], 0, 0, this);

		//Iterate and draw all the correlation edge
		try{

			//Copy temporary edges into the container in order to draw them all (mutual exclusive with temporary edge addition)
			edge_mutex.acquire();
			edges.addAll(edges_temp);
			edges_temp.clear();
			edge_mutex.release();

			//Draw edges contained in the main list (mutual exclusive with edge clearing)
			clean_mutex.acquire();
			Iterator<Edge> iter = edges.iterator();
			while(iter.hasNext()){

				//Get the two points
				Edge temp = iter.next();
				Point a = temp.getEndPoint(this.getWidth(),0);
				Point b = temp.getEndPoint(this.getWidth(),1);

				//Draw the two endpoints
				g.setColor(Color.blue);
				g.drawLine(a.x-3,a.y,a.x+3,a.y);
				g.drawLine(a.x,a.y-3,a.x,a.y+3);
				g.drawLine(b.x-3,b.y,b.x+3,b.y);
				g.drawLine(b.x,b.y-3,b.x,b.y+3);

				//Different color depending by positive or negative correlation
				if (temp.correlation>=0)
					g.setColor(Color.green);
				else
					g.setColor(Color.red);

				//Finally, draw the edge
				g.drawLine(a.x,a.y,b.x,b.y);
			}
			clean_mutex.release();

		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}

	}

	//Add an element to the list of edges to draw on the image
	public void addEdge(int x, int y, float correlation){

		//Create a new edge
		Edge temp = new Edge();
		temp.point[0]=x;
		temp.point[1]=y;
		temp.correlation=correlation;

		try{

			//Add the edge to the temporary list (mutual exclusive with copying temporary edges)
			edge_mutex.acquire();
			edges_temp.addLast(temp);
			edge_mutex.release();
		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	//Clear all the edges
	public void clearEdges(){

		try{

			//Clear temporary edges (mutual exclusive with adding temporary edges)
			edge_mutex.acquire();
			edges_temp.clear();
			edge_mutex.release();

			//Clear edges (mutual exclusive with drawing edges)
			clean_mutex.acquire();
			edges.clear();
			clean_mutex.release();

		} catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}