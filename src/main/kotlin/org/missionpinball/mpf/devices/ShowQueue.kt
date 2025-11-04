package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import java.util.*

/**
 * A show queue which plays shows sequentially.
 *
 * Shows are queued and played one after another. When a show finishes,
 * the next show in the queue starts automatically.
 */
class ShowQueue(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "show_queues"
    override val collection = "show_queues"
    override val classLabel = "show_queue"

    /**
     * Queue of shows to play.
     * Each entry contains a show config and start step.
     */
    private val showsQueue = ArrayDeque<Pair<ShowConfig, Int>>()

    /**
     * Currently playing show.
     */
    private var currentShow: RunningShow? = null

    /**
     * Add a show to the end of the queue.
     *
     * @param showConfig Configuration for the show to play
     * @param startStep Step to start the show at (default: 0)
     */
    fun enqueueShow(showConfig: ShowConfig, startStep: Int = 0) {
        showsQueue.add(Pair(showConfig, startStep))
        if (currentShow == null) {
            playNextShow()
        }
    }

    /**
     * Play the next show in the queue.
     */
    private fun playNextShow() {
        if (showsQueue.isEmpty()) {
            // No show queued
            currentShow = null
            return
        }

        val (showConfig, startStep) = showsQueue.removeFirst()

        // TODO: Implement show controller integration when available
        /*
        currentShow = machine.showController.replaceOrAdvanceShow(
            currentShow,
            showConfig,
            startStep = startStep,
            stopCallback = ::playNextShow
        )
        */

        debugLog("Playing next show from queue. ${showsQueue.size} shows remaining.")
    }
}

/**
 * Configuration for a show.
 *
 * TODO: Implement full show configuration when show system is available
 */
data class ShowConfig(
    val name: String,
    val priority: Int = 0,
    val speed: Double = 1.0,
    val loops: Int = 0,
    val sync_ms: Long? = null,
    val manual_advance: Boolean = false
)

/**
 * Represents a running show instance.
 *
 * TODO: Implement full running show when show system is available
 */
data class RunningShow(
    val config: ShowConfig,
    val currentStep: Int = 0,
    val isRunning: Boolean = true
)
