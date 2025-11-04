package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A speedometer to measure the speed of a ball.
 *
 * Measures the time it takes for a ball to travel between two switches
 * (start and stop) and posts an event with the delta time.
 */
class Speedometer(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "speedometers"
    override val collection = "speedometers"
    override val classLabel = "speedometer"

    /**
     * Timestamp when the start switch was hit.
     */
    private var timeStart: Double? = null

    override suspend fun deviceAddedSystemWide() {
        super.deviceAddedSystemWide()

        // Register switch handlers for start and stop switches
        val startSwitch = config["start_switch"] as? Switch
        val stopSwitch = config["stop_switch"] as? Switch

        if (startSwitch != null) {
            // TODO: Register switch handler when available
            /*
            machine.switchController.addSwitchHandlerObj(
                switch = startSwitch,
                callback = ::handleStartSwitch,
                state = 1
            )
            */
        }

        if (stopSwitch != null) {
            // TODO: Register switch handler when available
            /*
            machine.switchController.addSwitchHandlerObj(
                switch = stopSwitch,
                callback = ::handleStopSwitch,
                state = 1
            )
            */
        }
    }

    /**
     * Handle start switch activation.
     * Records the timestamp when the ball enters the measurement zone.
     */
    private fun handleStartSwitch() {
        val startSwitch = config["start_switch"] as? Switch
        timeStart = startSwitch?.lastChange
        debugLog("Start switch hit at $timeStart")
    }

    /**
     * Handle stop switch activation.
     * Calculates the time delta and posts an event with the speed measurement.
     */
    private fun handleStopSwitch() {
        val stopSwitch = config["stop_switch"] as? Switch
        val start = timeStart

        if (start != null && stopSwitch != null) {
            val delta = stopSwitch.lastChange - start
            timeStart = null

            debugLog("Stop switch hit. Delta: $delta seconds")

            // Post event with the time delta
            machine.events.post("${name}_hit", mapOf("delta" to delta))
            /**
             * Event: speedometer_(name)_hit
             *
             * The speedometer measured a ball passing through.
             *
             * Args:
             *   delta: Time in seconds between start and stop switches
             */
        }
    }
}
