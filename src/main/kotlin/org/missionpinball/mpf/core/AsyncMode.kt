package org.missionpinball.mpf.core

import kotlinx.coroutines.*

/**
 * Base class for async coroutine-based modes.
 *
 * AsyncMode extends Mode to support modes that run a long-running
 * coroutine for the duration of the mode's lifecycle. The coroutine
 * is automatically started when the mode starts and cancelled when
 * the mode stops.
 */
abstract class AsyncMode(
    machine: MachineController,
    config: MutableMap<String, Any?>,
    name: String,
    path: String,
    assetPaths: List<String>
) : Mode(machine, config, name, path, assetPaths) {

    /**
     * Coroutine job for the running mode task.
     */
    private var task: Job? = null

    /**
     * Coroutine scope for mode tasks.
     */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // TODO: Add callback when machine stop future is available
        // machine.stopFuture.addDoneCallback(::stopModeOnMachineStop)
    }

    /**
     * Stop mode because machine stopped.
     */
    private fun stopModeOnMachineStop() {
        task?.cancel()
        task = null
    }

    /**
     * Called when mode starts.
     * Launches the main coroutine task.
     */
    override fun started() {
        super.started()

        task = scope.launch {
            run()
        }.also { job ->
            job.invokeOnCompletion { exception ->
                when (exception) {
                    is CancellationException -> {
                        // Normal cancellation, ignore
                    }
                    null -> {
                        // Task completed normally
                    }
                    else -> {
                        // Task failed with exception, log it
                        logger.error(exception) { "AsyncMode task failed" }
                    }
                }
                // Stop mode
                stop()
            }
        }
    }

    /**
     * Called when mode stops.
     * Cancels the running coroutine task.
     */
    override fun stopped() {
        super.stopped()

        task?.cancel()
        task = null
    }

    /**
     * Main task which runs as long as the mode is active.
     *
     * Override this function in your mode. It's automatically
     * cancelled when the mode stops. You can catch CancellationException
     * to handle mode stop if needed.
     */
    abstract suspend fun run()

    /**
     * Stop the async mode and cancel all coroutines.
     */
    fun shutdown() {
        task?.cancel()
        scope.cancel()
    }
}
