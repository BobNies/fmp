package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import kotlin.math.max

/**
 * Represents a power supply unit (PSU) in a pinball machine.
 *
 * The PSU manages power delivery to coils and can throttle pulses
 * to prevent overloading the power supply. It tracks when the PSU
 * will be available and can delay pulses if needed.
 */
class PowerSupplyUnit(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "psus"
    override val collection = "psus"
    override val classLabel = "psu"

    /**
     * Time (in seconds since epoch) when the PSU will be free again.
     */
    private var busyUntil: Double? = null

    /**
     * Get wait time for a pulse, or 0 if it can be executed immediately.
     *
     * @param pulseMs Pulse duration in milliseconds
     * @param maxWaitMs Maximum time willing to wait in milliseconds
     * @return Wait time in milliseconds (0 if can pulse immediately)
     */
    fun getWaitTimeForPulse(pulseMs: Int, maxWaitMs: Int?): Int {
        val currentTime = machine.clock.getTime() / 1000.0  // Convert to seconds

        // Clear busy flag if it's in the past (prevent negative times)
        if (busyUntil != null && busyUntil!! < currentTime) {
            busyUntil = null
        }

        // If not busy or no max wait specified, pulse now
        if (busyUntil == null || maxWaitMs == null) {
            notifyAboutInstantPulse(pulseMs)
            return 0
        }

        // If busy for longer than max wait, pulse now anyway
        val maxWaitSec = maxWaitMs / 1000.0
        if (busyUntil!! > currentTime + maxWaitSec) {
            notifyAboutInstantPulse(pulseMs)
            return 0
        }

        // Calculate wait time and schedule the pulse
        val waitMs = ((busyUntil!! - currentTime) * 1000).toInt()
        val releaseWaitMs = (config["release_wait_ms"] as? Number)?.toInt() ?: 0
        busyUntil = busyUntil!! + (pulseMs + releaseWaitMs) / 1000.0

        return waitMs
    }

    /**
     * Notify PSU about an immediate pulse.
     *
     * Updates the busy time to account for the pulse and release wait time.
     *
     * @param pulseMs Pulse duration in milliseconds
     */
    fun notifyAboutInstantPulse(pulseMs: Int) {
        val currentTime = machine.clock.getTime() / 1000.0  // Convert to seconds
        val releaseWaitMs = (config["release_wait_ms"] as? Number)?.toInt() ?: 0
        val totalTimeMs = pulseMs + releaseWaitMs
        val totalTimeSec = totalTimeMs / 1000.0

        busyUntil = if (busyUntil != null) {
            max(busyUntil!!, currentTime + totalTimeSec)
        } else {
            currentTime + totalTimeSec
        }
    }
}
