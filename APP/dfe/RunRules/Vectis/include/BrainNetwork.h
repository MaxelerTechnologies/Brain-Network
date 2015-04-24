/**\file */
#ifndef SLIC_DECLARATIONS_BrainNetwork_H
#define SLIC_DECLARATIONS_BrainNetwork_H
#include "MaxSLiCInterface.h"
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define BrainNetwork_FREQUENCY (150)
#define BrainNetwork_NPIPES (8)


/*----------------------------------------------------------------------------*/
/*---------------------------- Interface default -----------------------------*/
/*----------------------------------------------------------------------------*/



/**
 * \brief Basic static function for the interface 'default'.
 * 
 * \param [in] ticks_ColumnMemoryControllerKernel The number of ticks for which kernel "ColumnMemoryControllerKernel" will run.
 * \param [in] ticks_CompressorBufferKernel The number of ticks for which kernel "CompressorBufferKernel" will run.
 * \param [in] ticks_LinearCorrelationKernel The number of ticks for which kernel "LinearCorrelationKernel" will run.
 * \param [in] inscalar_ColumnMemoryControllerKernel_n_active_points Input scalar parameter "ColumnMemoryControllerKernel.n_active_points".
 * \param [in] inscalar_CompressorBufferKernel_burst_offset Input scalar parameter "CompressorBufferKernel.burst_offset".
 * \param [in] inscalar_LinearCorrelationKernel_correlation_threshold Input scalar parameter "LinearCorrelationKernel.correlation_threshold".
 * \param [in] inscalar_LinearCorrelationKernel_n_active_points Input scalar parameter "LinearCorrelationKernel.n_active_points".
 * \param [out] outscalar_CompressorBufferKernel_n_active_edges Output scalar parameter "CompressorBufferKernel.n_active_edges".
 * \param [in] instream_from_host Stream "from_host".
 * \param [in] instream_size_from_host The size of the stream instream_from_host in bytes.
 * \param [out] outstream_to_host Stream "to_host".
 * \param [in] outstream_size_to_host The size of the stream outstream_to_host in bytes.
 * \param [in] lmem_address_dram_to_host Linear LMem control for "dram_to_host" stream: base address, in bytes.
 * \param [in] lmem_arr_size_dram_to_host Linear LMem control for "dram_to_host" stream: array size, in bytes.
 * \param [in] lmem_address_host_to_dram Linear LMem control for "host_to_dram" stream: base address, in bytes.
 * \param [in] lmem_arr_size_host_to_dram Linear LMem control for "host_to_dram" stream: array size, in bytes.
 * \param [in] lmem_address_row_bursts_from_DRAM Linear LMem control for "row_bursts_from_DRAM" stream: base address, in bytes.
 * \param [in] lmem_arr_size_row_bursts_from_DRAM Linear LMem control for "row_bursts_from_DRAM" stream: array size, in bytes.
 */
void BrainNetwork(
	uint64_t ticks_ColumnMemoryControllerKernel,
	uint64_t ticks_CompressorBufferKernel,
	uint64_t ticks_LinearCorrelationKernel,
	uint64_t inscalar_ColumnMemoryControllerKernel_n_active_points,
	uint64_t inscalar_CompressorBufferKernel_burst_offset,
	double inscalar_LinearCorrelationKernel_correlation_threshold,
	uint64_t inscalar_LinearCorrelationKernel_n_active_points,
	uint64_t *outscalar_CompressorBufferKernel_n_active_edges,
	const void *instream_from_host,
	size_t instream_size_from_host,
	void *outstream_to_host,
	size_t outstream_size_to_host,
	size_t lmem_address_dram_to_host,
	size_t lmem_arr_size_dram_to_host,
	size_t lmem_address_host_to_dram,
	size_t lmem_arr_size_host_to_dram,
	size_t lmem_address_row_bursts_from_DRAM,
	size_t lmem_arr_size_row_bursts_from_DRAM);

/**
 * \brief Basic static non-blocking function for the interface 'default'.
 * 
 * Schedule to run on an engine and return immediately.
 * The status of the run can be checked either by ::max_wait or ::max_nowait;
 * note that one of these *must* be called, so that associated memory can be released.
 * 
 * 
 * \param [in] ticks_ColumnMemoryControllerKernel The number of ticks for which kernel "ColumnMemoryControllerKernel" will run.
 * \param [in] ticks_CompressorBufferKernel The number of ticks for which kernel "CompressorBufferKernel" will run.
 * \param [in] ticks_LinearCorrelationKernel The number of ticks for which kernel "LinearCorrelationKernel" will run.
 * \param [in] inscalar_ColumnMemoryControllerKernel_n_active_points Input scalar parameter "ColumnMemoryControllerKernel.n_active_points".
 * \param [in] inscalar_CompressorBufferKernel_burst_offset Input scalar parameter "CompressorBufferKernel.burst_offset".
 * \param [in] inscalar_LinearCorrelationKernel_correlation_threshold Input scalar parameter "LinearCorrelationKernel.correlation_threshold".
 * \param [in] inscalar_LinearCorrelationKernel_n_active_points Input scalar parameter "LinearCorrelationKernel.n_active_points".
 * \param [out] outscalar_CompressorBufferKernel_n_active_edges Output scalar parameter "CompressorBufferKernel.n_active_edges".
 * \param [in] instream_from_host Stream "from_host".
 * \param [in] instream_size_from_host The size of the stream instream_from_host in bytes.
 * \param [out] outstream_to_host Stream "to_host".
 * \param [in] outstream_size_to_host The size of the stream outstream_to_host in bytes.
 * \param [in] lmem_address_dram_to_host Linear LMem control for "dram_to_host" stream: base address, in bytes.
 * \param [in] lmem_arr_size_dram_to_host Linear LMem control for "dram_to_host" stream: array size, in bytes.
 * \param [in] lmem_address_host_to_dram Linear LMem control for "host_to_dram" stream: base address, in bytes.
 * \param [in] lmem_arr_size_host_to_dram Linear LMem control for "host_to_dram" stream: array size, in bytes.
 * \param [in] lmem_address_row_bursts_from_DRAM Linear LMem control for "row_bursts_from_DRAM" stream: base address, in bytes.
 * \param [in] lmem_arr_size_row_bursts_from_DRAM Linear LMem control for "row_bursts_from_DRAM" stream: array size, in bytes.
 * \return A handle on the execution status, or NULL in case of error.
 */
max_run_t *BrainNetwork_nonblock(
	uint64_t ticks_ColumnMemoryControllerKernel,
	uint64_t ticks_CompressorBufferKernel,
	uint64_t ticks_LinearCorrelationKernel,
	uint64_t inscalar_ColumnMemoryControllerKernel_n_active_points,
	uint64_t inscalar_CompressorBufferKernel_burst_offset,
	double inscalar_LinearCorrelationKernel_correlation_threshold,
	uint64_t inscalar_LinearCorrelationKernel_n_active_points,
	uint64_t *outscalar_CompressorBufferKernel_n_active_edges,
	const void *instream_from_host,
	size_t instream_size_from_host,
	void *outstream_to_host,
	size_t outstream_size_to_host,
	size_t lmem_address_dram_to_host,
	size_t lmem_arr_size_dram_to_host,
	size_t lmem_address_host_to_dram,
	size_t lmem_arr_size_host_to_dram,
	size_t lmem_address_row_bursts_from_DRAM,
	size_t lmem_arr_size_row_bursts_from_DRAM);

/**
 * \brief Advanced static interface, structure for the engine interface 'default'
 * 
 */
typedef struct { 
	uint64_t ticks_ColumnMemoryControllerKernel; /**<  [in] The number of ticks for which kernel "ColumnMemoryControllerKernel" will run. */
	uint64_t ticks_CompressorBufferKernel; /**<  [in] The number of ticks for which kernel "CompressorBufferKernel" will run. */
	uint64_t ticks_LinearCorrelationKernel; /**<  [in] The number of ticks for which kernel "LinearCorrelationKernel" will run. */
	uint64_t inscalar_ColumnMemoryControllerKernel_n_active_points; /**<  [in] Input scalar parameter "ColumnMemoryControllerKernel.n_active_points". */
	uint64_t inscalar_CompressorBufferKernel_burst_offset; /**<  [in] Input scalar parameter "CompressorBufferKernel.burst_offset". */
	double inscalar_LinearCorrelationKernel_correlation_threshold; /**<  [in] Input scalar parameter "LinearCorrelationKernel.correlation_threshold". */
	uint64_t inscalar_LinearCorrelationKernel_n_active_points; /**<  [in] Input scalar parameter "LinearCorrelationKernel.n_active_points". */
	uint64_t *outscalar_CompressorBufferKernel_n_active_edges; /**<  [out] Output scalar parameter "CompressorBufferKernel.n_active_edges". */
	const void *instream_from_host; /**<  [in] Stream "from_host". */
	size_t instream_size_from_host; /**<  [in] The size of the stream instream_from_host in bytes. */
	void *outstream_to_host; /**<  [out] Stream "to_host". */
	size_t outstream_size_to_host; /**<  [in] The size of the stream outstream_to_host in bytes. */
	size_t lmem_address_dram_to_host; /**<  [in] Linear LMem control for "dram_to_host" stream: base address, in bytes. */
	size_t lmem_arr_size_dram_to_host; /**<  [in] Linear LMem control for "dram_to_host" stream: array size, in bytes. */
	size_t lmem_address_host_to_dram; /**<  [in] Linear LMem control for "host_to_dram" stream: base address, in bytes. */
	size_t lmem_arr_size_host_to_dram; /**<  [in] Linear LMem control for "host_to_dram" stream: array size, in bytes. */
	size_t lmem_address_row_bursts_from_DRAM; /**<  [in] Linear LMem control for "row_bursts_from_DRAM" stream: base address, in bytes. */
	size_t lmem_arr_size_row_bursts_from_DRAM; /**<  [in] Linear LMem control for "row_bursts_from_DRAM" stream: array size, in bytes. */
} BrainNetwork_actions_t;

/**
 * \brief Advanced static function for the interface 'default'.
 * 
 * \param [in] engine The engine on which the actions will be executed.
 * \param [in,out] interface_actions Actions to be executed.
 */
void BrainNetwork_run(
	max_engine_t *engine,
	BrainNetwork_actions_t *interface_actions);

/**
 * \brief Advanced static non-blocking function for the interface 'default'.
 *
 * Schedule the actions to run on the engine and return immediately.
 * The status of the run can be checked either by ::max_wait or ::max_nowait;
 * note that one of these *must* be called, so that associated memory can be released.
 *
 * 
 * \param [in] engine The engine on which the actions will be executed.
 * \param [in] interface_actions Actions to be executed.
 * \return A handle on the execution status of the actions, or NULL in case of error.
 */
max_run_t *BrainNetwork_run_nonblock(
	max_engine_t *engine,
	BrainNetwork_actions_t *interface_actions);

/**
 * \brief Group run advanced static function for the interface 'default'.
 * 
 * \param [in] group Group to use.
 * \param [in,out] interface_actions Actions to run.
 *
 * Run the actions on the first device available in the group.
 */
void BrainNetwork_run_group(max_group_t *group, BrainNetwork_actions_t *interface_actions);

/**
 * \brief Group run advanced static non-blocking function for the interface 'default'.
 * 
 *
 * Schedule the actions to run on the first device available in the group and return immediately.
 * The status of the run must be checked with ::max_wait. 
 * Note that use of ::max_nowait is prohibited with non-blocking running on groups:
 * see the ::max_run_group_nonblock documentation for more explanation.
 *
 * \param [in] group Group to use.
 * \param [in] interface_actions Actions to run.
 * \return A handle on the execution status of the actions, or NULL in case of error.
 */
max_run_t *BrainNetwork_run_group_nonblock(max_group_t *group, BrainNetwork_actions_t *interface_actions);

/**
 * \brief Array run advanced static function for the interface 'default'.
 * 
 * \param [in] engarray The array of devices to use.
 * \param [in,out] interface_actions The array of actions to run.
 *
 * Run the array of actions on the array of engines.  The length of interface_actions
 * must match the size of engarray.
 */
void BrainNetwork_run_array(max_engarray_t *engarray, BrainNetwork_actions_t *interface_actions[]);

/**
 * \brief Array run advanced static non-blocking function for the interface 'default'.
 * 
 *
 * Schedule to run the array of actions on the array of engines, and return immediately.
 * The length of interface_actions must match the size of engarray.
 * The status of the run can be checked either by ::max_wait or ::max_nowait;
 * note that one of these *must* be called, so that associated memory can be released.
 *
 * \param [in] engarray The array of devices to use.
 * \param [in] interface_actions The array of actions to run.
 * \return A handle on the execution status of the actions, or NULL in case of error.
 */
max_run_t *BrainNetwork_run_array_nonblock(max_engarray_t *engarray, BrainNetwork_actions_t *interface_actions[]);

/**
 * \brief Converts a static-interface action struct into a dynamic-interface max_actions_t struct.
 *
 * Note that this is an internal utility function used by other functions in the static interface.
 *
 * \param [in] maxfile The maxfile to use.
 * \param [in] interface_actions The interface-specific actions to run.
 * \return The dynamic-interface actions to run, or NULL in case of error.
 */
max_actions_t* BrainNetwork_convert(max_file_t *maxfile, BrainNetwork_actions_t *interface_actions);

/**
 * \brief Initialise a maxfile.
 */
max_file_t* BrainNetwork_init(void);

/* Error handling functions */
int BrainNetwork_has_errors(void);
const char* BrainNetwork_get_errors(void);
void BrainNetwork_clear_errors(void);
/* Free statically allocated maxfile data */
void BrainNetwork_free(void);
/* These are dummy functions for hardware builds. */
int BrainNetwork_simulator_start(void);
int BrainNetwork_simulator_stop(void);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* SLIC_DECLARATIONS_BrainNetwork_H */

