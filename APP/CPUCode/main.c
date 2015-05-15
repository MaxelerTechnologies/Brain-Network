/*********************************************************************
 * Maxeler Technologies: BrainNetwork                                *
 *                                                                   *
 * Version: 1.2                                                      *
 * Date:    05 July 2013                                             *
 *                                                                   *
 * DFE host-side code source file                                    *
 *                                                                   *
 *********************************************************************/

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <signal.h>
#include <stdint.h>
#include <time.h>
#include <sys/time.h>
#include <math.h>
#include <signal.h>
#include <string.h>
#include <dlfcn.h>

#include "slic_private/slic_private_type.h"
#include "BrainNetwork.h"

//Maximum number of device
#define MAX_N_DEVICES  1

static FILE *flog = NULL;
#define DEBUG(msg, args...) \
		if (flog != NULL) { \
			fprintf(flog, msg, ## args); \
			fprintf(flog, "\n"); \
			fflush(flog); \
		}

//Temporal window size
static int window = 30;

//Pipes to communicate with GUI
static FILE * input_pipe = NULL;
static FILE * output_pipe = NULL;

//DFE pipelines (zero means CPU execution)
static int pipes = 0;
static int frequency = 0;

// In order to construct a list of points
struct point_el {
	int pixel;
	float average;
	float standard_deviation;
	uint8_t * time_series;
	struct point_el * next;
};

typedef struct point_el point_t;

//Edges between points
struct edge {
	unsigned int pixel_a;
	unsigned int pixel_b;
	float correlation;
};

typedef struct edge edge_t;

//Useful information for managing memory burst
int bytes_per_burst = 0;
int points_per_burst = 0;
int padding = 0;



/* N.B. don't change this string without changing the corresponding string in the GUI!
 * Because stdout and stderr are used as communication channels with the GUI, we can't
 * simply print out error messages here and exit.
 * Instead, insert a special tag into error messages here, and check for them in the GUI.
 */
static const char const *err_tag = "Error!!!";
static void raise_error(const char *msg)
{
	fprintf(stderr, "%s: %s\n", err_tag, msg);
	fflush (stderr);
}

/* check for errors in SLiC entities, such as maxfile, engine, and actions. */
static void check_errors(max_errors_t *errors)
{
	// If there are any errors, write them to stderr stream for the GUI to handle
	if (!max_ok(errors)) {
		char *trace = max_errors_trace(errors);
		raise_error(trace);
		free(trace);
	}
}

//Create random data points
void create_random_points (char * random_file_name, int n){

	//Open a file
	int random_file = open(random_file_name, O_WRONLY | O_CREAT | O_APPEND, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
	if (random_file == -1) {
		fprintf(stderr,"Cannot create random file %s\n",random_file_name);
		exit(1);
	}

	//Temporary buffer for time series
	uint8_t * time_series_buffer = malloc(window  * sizeof(uint8_t));

	//Create n random points
	srand (time(NULL));
	for (int i=0; i<n;++i){

		//Store pixel number
		write(random_file,&i,sizeof(int));

		//Generate time series
		for(int j = 0; j < window; j++) {
			time_series_buffer[j] = rand() % 0x100;
		}

		//Calculate and store average
		float average=0;
		for (int j=0; j<window; ++j)
			average+=time_series_buffer[j];
		average=average/window;
		write(random_file,&average,sizeof(float));

		//Calculate and standard deviation
		float standard_deviation=0;
		for (int j=0; j<window; ++j)
			standard_deviation+= (time_series_buffer[j]-average)*(time_series_buffer[j]-average);
		standard_deviation = sqrt(standard_deviation/window);
		write(random_file,&standard_deviation,sizeof(float));

		//Store time series
		for (int j=0; j<window; ++j)
			write(random_file,time_series_buffer+j,sizeof(uint8_t));
	}

	//Close the file with terminator (point -1) and a correlation threshold
	int temp =-1;
	write(random_file,&temp,sizeof(int));
	float threshold = 0.5;
	write(random_file,&threshold,sizeof(float));

	//Close the file and deallocate time series buffer
	close(random_file);
	free(time_series_buffer);
}

//Read the list of points from the input pipe
point_t * read_input_pipe(int * n_points, float * threshold){

	//Pointers to store the list
	point_t * points_list = NULL;
	point_t * points_list_tail = NULL;

	//Initialize variable in order to count
	*n_points = 0;

	//Read points until terminator -1
	int int_buffer;
	fread(&int_buffer,sizeof(int),1,input_pipe);
	while(int_buffer!=-1){

		//Create a new point
		++(*n_points);
		point_t * current_point = malloc(sizeof(point_t));
		current_point->pixel=int_buffer;

		//Read average and standard deviation from pipe
		fread(&current_point->average,sizeof(float),1,input_pipe);
		fread(&current_point->standard_deviation,sizeof(float),1,input_pipe);

		//Read time series
		current_point->time_series= malloc(window * sizeof(uint8_t));
		fread(current_point->time_series,sizeof(uint8_t),window,input_pipe);

		//Add node as list tail from pipe
		current_point->next = NULL;
		if(!points_list)
			points_list=current_point;
		else
			points_list_tail->next=current_point;

		points_list_tail = current_point;

		//Read the next point from pipe
		fread(&int_buffer,sizeof(int),1,input_pipe);
	}

	//Read correlation threshold from pipe
	fread(threshold,sizeof(float),1,input_pipe);

	return points_list;
}

//Calculate linear correlation storing active edges on pipe
void cpu_calculate_linear_correlation(point_t * points_list, int n_points, float threshold){

	//Running time statistics
	struct timeval tim;
	gettimeofday(&tim, NULL);
	double start=tim.tv_sec+(tim.tv_usec/1000000.0);

	//Loop over the pairs of points
	point_t * temp_point = points_list;
	for (int i=0; i<n_points-1; ++i, temp_point=temp_point->next){

		//Loop over the remaining points
		point_t * other_temp_point = temp_point->next;
		for (int j=i+1; j<n_points; ++j, other_temp_point=other_temp_point->next){

			//Calculate covariance
			float cov=0;
			for (int k=0; k<window; ++k)
				cov+=temp_point->time_series[k]*other_temp_point->time_series[k];
			cov/=window;
			cov-= temp_point->average*other_temp_point->average;

			//Calculate correlation
			float correlation = cov / (temp_point->standard_deviation*other_temp_point->standard_deviation);

			//Check if active edge
			if (correlation>=threshold || correlation<=(-threshold)){

				fwrite(&temp_point->pixel,sizeof(int),1,output_pipe);
				fwrite(&other_temp_point->pixel,sizeof(int),1,output_pipe);
				fwrite(&correlation,sizeof(float),1,output_pipe);
				fflush(output_pipe);
			}
		}
	}

	//Calculate running time
	gettimeofday(&tim, NULL);
	double stop=tim.tv_sec+(tim.tv_usec/1000000.0);
	double cpu_time = stop-start;

	//Terminate pipe data with terminator point (0,0) and time statistic
	int int_buffer=0;
	fwrite(&int_buffer,sizeof(int),1,output_pipe);
	fwrite(&int_buffer,sizeof(int),1,output_pipe);
	fwrite(&cpu_time,sizeof(double),1,output_pipe);
	fflush(output_pipe);
	fprintf(stderr,"completed in %.6lf s\n",cpu_time);
}

//Used to write the DFE DRAM memory from host
static int write_memory( max_file_t *const maxfile,		//Max file created from DFE manager compilation
		max_engine_t *const device,						//The physical DFE device
		uint32_t* data,									//Data to transfer
		unsigned offset_bytes,							//Offset in bytes from the beginning of the memory
		unsigned data_length_bytes)						//Bytes to write
{
	max_actions_t *actions = max_actions_init(maxfile, NULL);
	max_disable_validation(actions);
	max_lmem_linear(actions, "host_to_dram", offset_bytes, data_length_bytes);
	max_queue_input(actions, "from_host", data, data_length_bytes);
	check_errors(actions->errors);

	max_run(device, actions);
	check_errors(device->errors);

	max_actions_free(actions);
	return data_length_bytes;
}

//Used to read the DFE DRAM memory from host
static uint32_t* read_memory(max_file_t *const maxfile,	//Max file created from DFE manager compilation
		max_engine_t *const device,						//The physical DFE device
		unsigned offset_bytes,							//Offset in bytes from the beginning of the memory
		unsigned data_length_bytes)						//Bytes to read
{
	max_actions_t *actions = max_actions_init(maxfile, NULL);
	max_disable_validation(actions);

	//Allocate a dynamic buffer
	uint32_t *const data_out = malloc(data_length_bytes);
	for (unsigned int i = 0; i < (data_length_bytes/sizeof(uint32_t)); ++i)
		data_out[i] = 0;

	max_lmem_linear (actions, "dram_to_host", offset_bytes, data_length_bytes);
	max_queue_output(actions, "to_host", data_out, data_length_bytes);
	check_errors(actions->errors);

	max_run(device, actions);
	check_errors(device->errors);

	max_actions_free(actions);
	return data_out;
}

//Create a stream of data points given a list
void * create_stream_buffer(point_t * points_list, int n_points, int n_bursts){

	//Use fixed point representation
	HWType fixed_point_type = hwFix(8,16,UNSIGNED);

	//Convert list to a stream buffer
	void * stream_buffer = malloc(bytes_per_burst * n_bursts);
	void * buffer_pointer = stream_buffer;
	int written_point = 0;
	for (int i=0; i<n_bursts; ++i){

		//Store the current burst
		int j=0;
		while(j<points_per_burst && written_point<n_points){

			//Store pixel position
			*((uint32_t *) buffer_pointer) = points_list->pixel;
			buffer_pointer+=4;

			//Store average as fixed point
			uint64_t fixed_point = max_convert_type(NULL,points_list->average,fixed_point_type);
			*((uint64_t *) buffer_pointer) = fixed_point;
			buffer_pointer+=3;

			//Store standard deviation as fixed point
			fixed_point = max_convert_type(NULL,points_list->standard_deviation,fixed_point_type);
			*((uint64_t *) buffer_pointer) = fixed_point;
			buffer_pointer+=3;

			//Store temporal series
			for (int k=0; k<window; ++k, ++buffer_pointer)
				*((uint8_t *) buffer_pointer) = points_list->time_series[k];

			//Go to the next active point
			points_list=points_list->next;
			++j;
			++written_point;
		}

		//Add padding to complete the burst
		buffer_pointer+= padding;
	}

	return stream_buffer;
}

//Greatest common divisor (recursive Euclid's algorithm)
int gcd(int a, int b){

	//Stop recursion
	if (b==0)
		return a;

	//Recursive call
	return gcd(b,a%b);
}

//Calculate linear correlation storing active edges on pipe
void dfe_calculate_linear_correlation(max_file_t * maxfile, max_engine_t * device, point_t * points_list, int n_points, float threshold){

	max_actions_t *actions = max_actions_init(maxfile, NULL);
	max_disable_validation(actions);

	//Calculate number of bursts
	int n_bursts =  n_points/points_per_burst;
	if (n_points%points_per_burst)
		++n_bursts;

	//Create a stream of data points
	void * stream_buffer = create_stream_buffer(points_list,n_points,n_bursts);

	//Load the data onto the DRAM before computing linear correlation
	struct timeval tim;
	gettimeofday(&tim, NULL);
	double start=tim.tv_sec+(tim.tv_usec/1000000.0);
	write_memory(maxfile, device, stream_buffer,0,bytes_per_burst*n_bursts);
	gettimeofday(&tim, NULL);
	double stop=tim.tv_sec+(tim.tv_usec/1000000.0);
	double dfe_time = stop-start;

	//Set the number of active points
	max_set_uint64t(actions, "ColumnMemoryControllerKernel", "n_active_points", n_points);
	max_set_uint64t(actions, "LinearCorrelationKernel", "n_active_points", n_points);

	//Set the correlation threshold
	max_set_double(actions, "LinearCorrelationKernel", "correlation_threshold", threshold);

	//Set the burst offset where to write the result
	max_set_uint64t(actions, "CompressorBufferKernel", "burst_offset", n_bursts);

	// Set up address generators for the row bursts
	max_lmem_linear(actions, "row_bursts_from_DRAM", 0, bytes_per_burst*n_bursts);
	max_lmem_set_interrupt_on(actions, "burst_to_memory");

	//Calculate the necessary clock cycles for the linear correlation kernel
	unsigned long clock_cycles = (pipes-1)*((n_points-1)/pipes);
	clock_cycles+= (((n_points*(n_points-1))/2)-((n_points-1)/pipes)*((pipes*pipes)/2-pipes/2)
			-((((((n_points-1)%pipes)+1)*(((n_points-1)%pipes)+1))/2) - (((n_points-1)%pipes)+1)/2))/pipes;
	clock_cycles+= (n_points-1)%pipes;

	//Calculate Least Common Divisor
	int LCM = (pipes * (bytes_per_burst/12)) / gcd(pipes,(bytes_per_burst/12));
	unsigned long  extra_clock_cycle = LCM/pipes;

	//Run the kernel
	max_set_ticks(actions,"ColumnMemoryControllerKernel",clock_cycles);
	max_set_ticks(actions,"LinearCorrelationKernel",clock_cycles+extra_clock_cycle);
	max_set_ticks(actions,"CompressorBufferKernel",clock_cycles+extra_clock_cycle);
	uint64_t n_edges = 0;
	max_get_uint64t(actions, "CompressorBufferKernel", "n_active_edges", &n_edges);
	check_errors(actions->errors);

	gettimeofday(&tim, NULL);
	start=tim.tv_sec+(tim.tv_usec/1000000.0);
	max_run(device, actions);
	gettimeofday(&tim, NULL);
	stop=tim.tv_sec+(tim.tv_usec/1000000.0);
	dfe_time += stop-start;

	check_errors(device->errors);
	max_actions_free(actions);

	/*
	DEBUG("======\nn_edges=%ld", n_edges);
	sleep(1);
	DEBUG("n_edges=%ld\n======", n_edges);*/

	//Reading from DFE memory
	gettimeofday(&tim, NULL);
	start=tim.tv_sec+(tim.tv_usec/1000000.0);
	unsigned long bytes_to_read = n_edges * sizeof(edge_t);
	bytes_to_read += (bytes_to_read%bytes_per_burst) ? (bytes_per_burst-(bytes_to_read%bytes_per_burst)) : 0;
	edge_t * edges_array  = NULL;
	if (n_edges)
		edges_array  = (edge_t *) read_memory(maxfile, device, bytes_per_burst*n_bursts, bytes_to_read);
	gettimeofday(&tim, NULL);
	stop=tim.tv_sec+(tim.tv_usec/1000000.0);
	dfe_time += stop-start;

    //Copy the result on the data pipe
    for (uint64_t i=0; i<n_edges; ++i){

    	fwrite(&(edges_array[i].pixel_a),sizeof(int),1,output_pipe);
    	fwrite(&(edges_array[i].pixel_b),sizeof(int),1,output_pipe);
    	fwrite(&(edges_array[i].correlation),sizeof(float),1,output_pipe);
    	fflush(output_pipe);
    }

    //Terminate data pipe with terminator point (0,0) and time statistic
    int int_buffer=0;
    fwrite(&int_buffer,sizeof(int),1,output_pipe);
    fwrite(&int_buffer,sizeof(int),1,output_pipe);
    fwrite(&dfe_time,sizeof(double),1,output_pipe);
    fflush(output_pipe);
    fprintf(stderr,"completed in %.6lf s\n",dfe_time);

    //Deallocate points and edges
    free(stream_buffer);
    if (n_edges)
    	free(edges_array);
}

//Deallocate list from heap memory
point_t * deallocate_points(point_t * points_list){

	//Remove point one by one
	while (points_list){

		//Remove head node from list
		point_t * temp_point = points_list;
		points_list=points_list->next;

		//Deallocate node memory
		free(temp_point->time_series);
		free(temp_point);
	}
	return NULL;
}

//Redefine SIGUSR1 handle
void SIGUSR1_handler(int sig_no __attribute__((unused)))
{
	//Do nothing instead of a printout
}

//Silent handler for probing busy devices
void silent_error_handler(max_engine_t *const device __attribute__((unused)), const char * message __attribute__((unused)), const char * trace __attribute__((unused)))
{

}

int main(int argc, char* argv[])
{
	if (getenv("LAUNCH_FROM_SCRIPT") == NULL) {
		char *buf   = strdup(argv[0]);
		char *slash = strrchr(buf, '/');
		if (slash) {
			strcpy(slash, "");
			char cmd[1024];
			snprintf(cmd, 1024, "./run.sh %s", buf);
			putenv("LAUNCH_FROM_SCRIPT");
			system(cmd);
		}
		free(buf);
		return 0;
	}

	//Read the command line
	int option = 0;
	char * library = "libdfe.so";
	while ((option = getopt (argc, argv, "xw:l:")) != -1){
		switch (option){

			//Accelerate daemon with DFE
			case 'x':
				pipes = 1;
				break;

			//Optional window size (default window=30)
			case 'w':
				window = atoi(optarg);
				break;

			//Dynamic library directory
			case 'l':
				library = optarg;
				break;
			default:
				break;
		}
	}

	//Get process ID
	int pid = getpid();
	fprintf(stderr,"Process ID... %d\n",pid);

	//Flag to decide between DFE and CPU
	char valid_device = 0;

	//Open shared library with DFE bitstream
	void * shared_library = dlopen(library, RTLD_NOW);

	max_file_t   *maxfile = NULL;
	max_engine_t *engine  = NULL;

	if (pipes)
		fprintf(stderr,"Trying to connect to DFE... ");
	else
		fprintf(stderr,"Running on CPU...\n");

	if (pipes && shared_library) {
		max_file_t * (*max_maxfile_init)() = dlsym(shared_library, "BrainNetwork_init");

		if (max_maxfile_init) {
			maxfile = (*max_maxfile_init)();
			if (maxfile == NULL) {
				raise_error("Unable to initialize maxfile!");
			}

			// Note that we disable the abort-on-error SLiC behaviour: otherwise this c process might abort and the GUI waits forever.
			max_errors_mode(maxfile->errors, 0);
			engine = max_load(maxfile, "*");

			//If successful
			if (engine) {
				//Get the number of pipes
				pipes     = BrainNetwork_NPIPES;
				frequency = BrainNetwork_FREQUENCY;

				//Get burst size
				bytes_per_burst  = max_get_burst_size(maxfile, NULL);

				//Calculate how many point per burst
				points_per_burst = bytes_per_burst/(10+window);

				//Calculate padding
				padding = bytes_per_burst - (10+window) * points_per_burst;

				valid_device = 1;
				fprintf(stderr, "running on DFE with %d pipelines at %dMHz...\n", pipes, frequency);
			} else {
				raise_error("Unable to load device!\n");
				check_errors(maxfile->errors);
			}
		}
	}

	//Print message
	if (!valid_device && pipes) {
		fprintf(stderr,"DFE not found, running on CPU\n");
	}

	//Use stdin and stdout as communication communication pipes
	input_pipe = stdin;
	output_pipe = stdout;

	//Stream the pid within the pipes
	fwrite(&pid,sizeof(int),1,output_pipe);
	fflush(output_pipe);

	//Signal SIGUSR1 is used for triggering the computation
	int signal;
	sigset_t signal_mask;
	sigemptyset(&signal_mask);
	sigaddset(&signal_mask, SIGUSR1);

	//Redefine SIGUSR1 handler
	struct sigaction sa;
	memset(&sa, 0, sizeof(sa));
	sa.sa_handler = &SIGUSR1_handler;
	sigaction(SIGUSR1, &sa,NULL);

	if (valid_device && pipes && shared_library)
		flog = fopen("dfe.out", "w");

	//Linear correlation daemon
	while(1){

		//Wait for starting signal
		sigwait(&signal_mask, &signal);

		//Reading from pipe
		int n_points;
		float threshold;
		fprintf(stderr,"Reading from pipe... ");
		point_t * points_list = read_input_pipe(&n_points,&threshold);
		fprintf(stderr,"completed\n");

		//Computing linear correlation using DFE or CPU
		fprintf(stderr,"Computing on %d points with correlation %.3lf... ",n_points,threshold);
		if (valid_device && pipes && shared_library)
			dfe_calculate_linear_correlation(maxfile,engine,points_list,n_points,threshold);
		else
			cpu_calculate_linear_correlation(points_list,n_points,threshold);

		//Deallocate memory
		fprintf(stderr,"Deallocate memory... ");
		points_list = deallocate_points(points_list);
		fprintf(stderr,"completed\n");
		//break;
	}

	return 0;
}
