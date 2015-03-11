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

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.Semaphore;

//Describe a temporal series on a point (pixel)
class BrainPoint{

	//Pixel (x,y) described as a 1D point identifier
	public int point;

	//Temporal series with average and standard deviation
	public float average = 0;
	public float standard_deviation = 0;
	public byte [] temporal_series = null;

	//Constructor
	public BrainPoint(int point, byte [] series){

		//Get point identifier
		this.point = point;

		//Copy the temporal series
		temporal_series = series;

		//Derive average
		for (int j=0; j<temporal_series.length; ++j)
			average += (temporal_series[j] & 0xff);
		average/=temporal_series.length;

		//Derive standard deviation
		for (int j=0; j<temporal_series.length; ++j)
			standard_deviation += ((temporal_series[j] & 0xff) - average) * ((temporal_series[j] & 0xff) - average);
		standard_deviation /= temporal_series.length;
		standard_deviation = (float)Math.sqrt(standard_deviation);
    }
}

public class LinearCorrelation implements Runnable, WindowListener{

	//Used to read/write on a pipe connected to a C process
	private static int convertLittleEndian(int temp){

		//Get four bytes from integers
		byte [] b = new byte[4];
		b[0] = (byte)(temp);
		b[1] = (byte)(temp >> 8);
		b[2] = (byte)(temp >> 16);
		b[3] = (byte)(temp >> 24);

		//Convert to little endian
		temp = (b[3]&0xff);
		temp += (b[2]&0xff) << 8;
		temp += (b[1]&0xff) << 16;
		temp += (b[0]&0xff) << 24;

		return temp;
	}

	//Used to read/write on a pipe connected to a C process
	private static float convertLittleEndian(float temp){

		//Get bits as integer
		int bits = Float.floatToIntBits(temp);

		//Get four bytes from integers
		byte [] b = new byte[4];
		b[0] = (byte)(bits);
		b[1] = (byte)(bits >> 8);
		b[2] = (byte)(bits >> 16);
		b[3] = (byte)(bits >> 24);

		//Convert to little endian
		bits = (b[3]&0xff);
		bits += (b[2]&0xff) << 8;
		bits += (b[1]&0xff) << 16;
		bits += (b[0]&0xff) << 24;

		return Float.intBitsToFloat(bits);
	}

	//Used to read/write on a pipe connected to a C process
	private static double convertLittleEndian(double temp){

		//Get bits as integer
		long bits = Double.doubleToLongBits(temp);

		//Get four bytes from integers
		byte [] b = new byte[8];
		b[0] = (byte)(bits);
		b[1] = (byte)(bits >> 8);
		b[2] = (byte)(bits >> 16);
		b[3] = (byte)(bits >> 24);
		b[4] = (byte)(bits >> 32);
		b[5] = (byte)(bits >> 40);
		b[6] = (byte)(bits >> 48);
		b[7] = (byte)(bits >> 56);

		//Convert to little endian
		bits = (((long)b[7])&0xff);
		bits += (((long)b[6])&0xff) << 8;
		bits += (((long)b[5])&0xff) << 16;
		bits += (((long)b[4])&0xff) << 24;
		bits += (((long)b[3])&0xff) << 32;
		bits += (((long)b[2])&0xff) << 40;
		bits += (((long)b[1])&0xff) << 48;
		bits += (((long)b[0])&0xff) << 56;

		return Double.longBitsToDouble(bits);
	}

	//Pipes to communicate
	private DataOutputStream data = null;
	private DataInputStream result = null;
	private BufferedReader text = null;

	// This string must match up with the c code; all very ugly, but no worse than using stderr as a comms channel!
	private static final String err_tag = "Error!!!";
	private static final boolean dbg = false;

	//Used to write a float on a data pipe connected to a C process
	private static void writeLittleEndian(DataOutputStream data, float temp) throws IOException{

		//Get bits as integer
		int bits = Float.floatToIntBits(temp);

		//Get four bytes from ints
		byte [] b = new byte[4];
		b[0] = (byte)(bits);
		b[1] = (byte)(bits >> 8);
		b[2] = (byte)(bits >> 16);
		b[3] = (byte)(bits >> 24);

		//Convert to little endian
		data.write(b,0,4);
	}

	//Daemon process descriptor
	private Process daemon = null;
	private int daemon_pid = 0;
	private ShutDown shutdown_daemon = null;

	//Computation pipes for the underlying correlation engine (0 = CPU)
	protected boolean cpu;

	//Kill the daemon before ending
	class ShutDown extends Thread{

		//Reference to daemon process to kill
		private Process daemon = null;

		//Pipes to shut down
		private DataOutputStream data = null;
		private DataInputStream result = null;
		private BufferedReader text = null;

		//Constructor
		public ShutDown(Process daemon, DataOutputStream data, DataInputStream result, BufferedReader text){
			this.daemon = daemon;
			this.data = data;
			this.result = result;
			this.text = text;
		}

		//Thread simply closes pipes and kills the daemon
		@Override
		public void run(){
			try {
				if (data!=null)
					data.close();
				if (result!=null)
					result.close();
				if (text!=null)
					text.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			daemon.destroy();
        }

	}

	//References to loop and time field
	private ImageLoop l = null;
	private TimeTextField time = null;

	//Wrapper for the daemon
	public LinearCorrelation(ImageLoop l, TimeTextField time, boolean cpu){

		//Copy references
		this.l = l;
		this.time = time;

		//Launch linear correlation daemon
		this.cpu=cpu;
		ProcessBuilder daemon_builder = null;
		if (cpu)
			daemon_builder = new ProcessBuilder(
					/*"/network-raid/opt/valgrind/3.7.0/bin/valgrind",
					"--log-file=cpu.out",*/
					BrainNetwork.daemon_file,"-l",BrainNetwork.library_file);
		else
			daemon_builder = new ProcessBuilder(
					/*"/network-raid/opt/valgrind/3.7.0/bin/valgrind",
					"--log-file=valgrind.out",
					"--vgdb=yes",
					"--suppressions=/home/jbrobertson/svn/trunk/distribution/MaxCompiler/lib/slic.supp",*/
					BrainNetwork.daemon_file,"-x","-l",BrainNetwork.library_file);

		//Equivalent of calling maxldlib
		Map<String, String> env = daemon_builder.environment();
		String ldlib = env.get("LD_LIBRARY_PATH");
		String maxldlib = env.get("MAXELEROSDIR")+"/lib:"+ldlib;
		env.put("LD_LIBRARY_PATH", maxldlib);

		try {

			//Lauch the daemon
			daemon = daemon_builder.start();

			//Register daemon to shut down at termination
			shutdown_daemon = new ShutDown(daemon,data,result,text);
			Runtime.getRuntime().addShutdownHook(shutdown_daemon);

			//Connect pipes in order to communicate
			data = new DataOutputStream(daemon.getOutputStream());
			result = new DataInputStream(daemon.getInputStream());
			text = new BufferedReader(new InputStreamReader(daemon.getErrorStream()));

			//Wait result from pipe
			int n = result.available();
			while (n==0)
				n = result.available();

			//Read daemon pid
			daemon_pid = convertLittleEndian(result.readInt());
			if(dbg) System.err.println("DBG>  result: n=" + n + "   daemon_pid=" + daemon_pid);

			//Print message from daemon
			checkDaemonMsg(true);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void checkDaemonMsg(boolean echo) {
		try {
			for (int i=0; i<2; ++i) {
				String line = text.readLine();
				if (echo) System.out.println(line);
				if (dbg)  System.err.println("DBG>  text: " + line);

				// check for problems in the c process; this can happen if maxfile and device could not be initialised
				if (line.contains(err_tag)) {     // Syphon off the error trace, inform the hapless user, and exit
					while (text.ready()) {
						System.err.println("\t" + text.readLine());
					}
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	//Non-blocking synchronization... if one computation misses the deadline (it is still processing while the following is triggered)
	Semaphore mutex = new Semaphore(1,true);

	public void restart() {

		try{
			//Try to get the mutex... if available it means that thread is not processing
			if (mutex.tryAcquire()){

				//Just return
				mutex.release();
				return;
			}

			//Some thread is processing... close daemon
			daemon.destroy();
			Runtime.getRuntime().removeShutdownHook(shutdown_daemon);

			//To restart, simply call again the constructor
			ProcessBuilder daemon_builder = null;
			if (cpu)
				daemon_builder = new ProcessBuilder(BrainNetwork.daemon_file,"-l",BrainNetwork.library_file);
			else
				daemon_builder = new ProcessBuilder(BrainNetwork.daemon_file,"-x","-l",BrainNetwork.library_file);

			//Equivalent of calling maxldlib
			Map<String, String> env = daemon_builder.environment();
			String ldlib = env.get("LD_LIBRARY_PATH");
			String maxldlib = env.get("MAXELEROSDIR")+"/lib:"+ldlib;
			env.put("LD_LIBRARY_PATH", maxldlib);

			//Lauch the daemon
			daemon = daemon_builder.start();

			//Register daemon to shut down at termination
			shutdown_daemon = new ShutDown(daemon,data,result,text);
			Runtime.getRuntime().addShutdownHook(shutdown_daemon);

			//Connect pipes in order to communicate
			data = new DataOutputStream(daemon.getOutputStream());
			result = new DataInputStream(daemon.getInputStream());
			text = new BufferedReader(new InputStreamReader(daemon.getErrorStream()));

			//Wait result from pipe
			int n = result.available();
			while (n==0)
				n = result.available();

			//Read daemon pid
			daemon_pid = convertLittleEndian(result.readInt());
			if (dbg) System.err.println("DBG>  result: n=" + n + "   daemon_pid=" + daemon_pid);

			//Print message from daemon
			checkDaemonMsg(false);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	//Decide thresholds for considering active points and active edges
	public float point_threshold = (float)4.5;
	public float edge_threshold = (float)0.78;

	//Wrapper for the processing method
	private void processing(){

		//Check the non-blocking mutex
		if (!mutex.tryAcquire())
			return;

		/* Any c-side errors are transmitted here via stderr; at this point we are not expecting
		 * to receive a read acknowledgment, so check any stderr messages for errors.
		 * (A common one here is "MaxelerOS loaded driver revision (x) differs from libmaxeleros
		 * revision (y)", which gets lost otherwise, leaving the hapless user baffled).
		 */
		try {
			if (text.ready()) {
				String line = text.readLine();
				if (line.contains(err_tag)) {
					System.err.println("\t" + line);
					while (text.ready())
						System.err.println("\t" + text.readLine());
					System.exit(1);
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace(System.err);
		}

		//Get the current frame
		int frame = BrainNetwork.frame;

		//Measure the image size
		int height = l.images[frame].getHeight();
		int width = l.images[frame].getWidth();

		//Send signal to activate computation on the daemon
		try {
			Runtime.getRuntime().exec("kill -10 "+daemon_pid);
		} catch (Exception e) {
			mutex.release();
			return;
		}

		//Stream active point to the daemon
		for (int y=0; y< height; ++y){
			for (int x=0; x< width ; ++x){

				//Create a buffer for the temporal series, considering a window of 30 elements
				byte [] temp = new byte [30];
				for (int i=0; i<30; ++i){

					//Get the index of the past images, considering wrapping
					int index = frame-i;
					index = (index>=0) ? index : index+l.images.length;

					//Extract pixel value
					temp[i] = (byte)((l.images[(index)%(l.images.length)].getRGB(x, y)) & 0xff);
				}

				try{

					//Stream the point to the daemon, if necessary
					//BrainPoint p = new BrainPoint(y * width + x,temp);
					BrainPoint p = new BrainPoint(y * width + x,temp);
					if (p.standard_deviation>=point_threshold){
						data.writeInt(convertLittleEndian(p.point));
						writeLittleEndian(data,p.average);
						writeLittleEndian(data,p.standard_deviation);
						data.write(temp, 0, 30);
						data.flush();
					}
				} catch (Exception e) {
					mutex.release();
					return;
				}
			}

		}

		try {

			//Send terminator
			data.writeInt(convertLittleEndian(-1));
			writeLittleEndian(data,edge_threshold);
			data.flush();

			//Get reading acknowledge
			String line = text.readLine();
			if (dbg) System.err.println("DBG>  text(processing1): " + line);
		} catch (Exception e) {
			mutex.release();
			return;
		}


		//Delete previous edges
		l.clearEdges();
		try {

			//Now wait for results from daemon
			while(result.available()==0);
			int point_a =convertLittleEndian(result.readInt());
			int point_b =convertLittleEndian(result.readInt());

			//Until terminator (0,0)
			while (!(point_a==0 && point_b==0)){

				//Read correlation
				float correlation = convertLittleEndian(result.readFloat());

				//Add the edge if necessary
				l.addEdge(point_a, point_b, correlation);

				//Read next edge
				point_a =convertLittleEndian(result.readInt());
				point_b =convertLittleEndian(result.readInt());
			}

			//Get time and update text field
			time.setTime(convertLittleEndian(result.readDouble()));

			//Get writing acknowledge
			for (int i=0; i<2; ++i) {
				String line = text.readLine();
				if (dbg) System.err.println("DBG>  text(processing2): " + line);
			}

		} catch (Exception e) {
			if (dbg) System.err.println("DBG>  exception in processing: " + e.getMessage());
			mutex.release();
			return;
		}

		//Release the resource
		mutex.release();
	}

	@Override
	public void run(){

		//Call the synchronized processing
		processing();
	}

	@Override
	public void windowActivated(WindowEvent e) {

	}

	@Override
	public void windowClosed(WindowEvent e) {

	}

	@Override
	public void windowClosing(WindowEvent e){

		daemon.destroy();
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e){

	}

	@Override
	public void windowDeiconified(WindowEvent e){

	}

	@Override
	public void windowIconified(WindowEvent e){

	}

	@Override
	public void windowOpened(WindowEvent e){

	}

}
