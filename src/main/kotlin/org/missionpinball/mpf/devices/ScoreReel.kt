package org.missionpinball.mpf.devices

import kotlinx.coroutines.*
import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Represents an individual electro-mechanical score reel in a pinball machine.
 *
 * Multiple reels of this class can be grouped together into ScoreReelGroups
 * which collectively make up a display like "Player 1 Score" or "Player 2
 * card value", etc.
 *
 * This device class is used for all types of mechanical number reels in a
 * machine, including reels that have more than ten numbers and that can move
 * in multiple directions (such as the credit reel).
 */
class ScoreReel(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "score_reels"
    override val collection = "score_reels"
    override val classLabel = "score_reel"

    /**
     * Delay manager for timing operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * List with each element corresponding to a value on the reel.
     * An entry of null means there's no value switch there.
     */
    private val valueSwitches = mutableListOf<Switch?>()

    /**
     * The assumed value the machine thinks this reel is showing.
     * A value of -999 indicates that the value is unknown.
     */
    var assumedValue = -999

    /**
     * Holds the index of the destination the reel is trying to advance to.
     */
    private var destinationValue = 0

    /**
     * Coroutine job which advances the reel.
     */
    private var runner: Job? = null

    /**
     * Event that will be cleared when the runner is done. Set to trigger the runner.
     */
    private val busy = CompletableDeferred<Unit>()

    /**
     * Event that will be set when this reel is ready and shows the destination value.
     */
    private val ready = CompletableDeferred<Unit>()

    init {
        // Stop device on shutdown
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("shutdown", ::stop)
        */
        busy.complete(Unit)
    }

    override suspend fun initialize() {
        super.initialize()
        debugLog("Configuring score reel with: $config")

        // Figure out how many values we have
        // Add 1 so range is inclusive of the lower limit
        val limitLo = (config["limit_lo"] as? Number)?.toInt() ?: 0
        val limitHi = (config["limit_hi"] as? Number)?.toInt() ?: 9
        val numValues = limitHi - limitLo + 1

        debugLog("Total reel values: $numValues")

        for (value in 0 until numValues) {
            valueSwitches.add(config["switch_$value"] as? Switch)
        }

        runner = machine.launch {
            run()
        }
    }

    /**
     * Stop device.
     */
    fun stop() {
        runner?.cancel()
    }

    /**
     * Check all the value switches for this score reel.
     *
     * This check only happens if ready is true. If the reel is not
     * ready, it means another advance request has come in after the initial
     * one. In that case then the subsequent advance will call this method
     * again after that advance is done.
     *
     * If this method finds an active switch, it sets assumedValue to
     * that. Otherwise it sets it to -999.
     */
    fun checkHwSwitches() {
        // Check to make sure the hw_confirm_time time has passed
        val hwConfirmTime = (config["hw_confirm_time"] as? Number)?.toLong() ?: 300L

        debugLog("Checking hw switches to determine reel value with hw_confirm_time ${hwConfirmTime}ms")

        for ((i, switch) in valueSwitches.withIndex()) {
            if (switch != null) {
                // TODO: Check switch when available
                /*
                if (machine.switchController.isActive(switch, ms = hwConfirmTime)) {
                    if (assumedValue != i) {
                        infoLog("Setting value to $i because that switch is active.")
                        if (assumedValue != -999) {
                            log.warning("Reel de-synced. Assumed: $assumedValue. Real: $i")
                        }

                        assumedValue = i
                        busy.complete(Unit)
                        ready = CompletableDeferred()
                    }
                    return
                }
                */
            }
        }

        // Check if there is a switch for the current assumedValue
        if (assumedValue >= 0 && valueSwitches.getOrNull(assumedValue) != null) {
            // TODO: Check switch when available
            /*
            if (!machine.switchController.isActive(valueSwitches[assumedValue]!!, ms = hwConfirmTime)) {
                log.warning("Resetting value because the switch for $assumedValue is not active.")
                assumedValue = -999
                busy.complete(Unit)
                ready = CompletableDeferred()
            }
            */
        }
    }

    /**
     * Main run loop that manages reel advances.
     */
    private suspend fun run() {
        // TODO: Wait for event when available
        /*
        machine.events.waitForEvent("init_phase_3")
        */

        checkHwSwitches()

        while (isActive) {
            // Wait for either a new value or a switch change
            // TODO: Implement switch waiting when available
            /*
            val switchChangeFuture = machine.switchController.waitForAnySwitch(
                switches = valueSwitches.filterNotNull(),
                state = 2,
                ms = (config["hw_confirm_time"] as? Number)?.toLong() ?: 300L
            )
            val result = Util.first(listOf(switchChangeFuture, busy.await()))

            if (result == switchChangeFuture) {
                checkHwSwitches()
                continue
            }
            */

            busy.await()

            // Advance the reel until we reached our destination position
            advanceReelIfPositionDoesNotMatch()
        }
    }

    /**
     * Advance reel if the destination value has not been reached.
     */
    private suspend fun advanceReelIfPositionDoesNotMatch() {
        // Check if there is any need to do something
        if (destinationValue == assumedValue) {
            debugLog("Reel is already at value $destinationValue (will not move)")
            ready.complete(Unit)
            return
        }

        val repeatPulseTime = (config["repeat_pulse_time"] as? Number)?.toLong() ?: 200L
        debugLog("Advancing reel to value $destinationValue (current value: $assumedValue repeat_pulse_time: ${repeatPulseTime}ms)")

        while (destinationValue != assumedValue) {
            machine.events.post("reel_${name}_will_advance")

            val coilInc = config["coil_inc"] as? Driver
            // TODO: Pulse coil when available
            /*
            val waitMs = coilInc?.pulse(maxWaitMs = 500) ?: 0
            */
            val waitMs = 100 // Placeholder

            machine.events.post("reel_${name}_advancing")
            val previousValue = assumedValue

            delay((waitMs + repeatPulseTime))

            if (assumedValue >= 0) {
                assumedValue = (assumedValue + 1) % valueSwitches.size
            }

            checkHwSwitches()

            if (previousValue != assumedValue && assumedValue >= 0) {
                machine.events.post("reel_${name}_advanced")
                /**
                 * Event: reel_(name)_advanced
                 *
                 * The reel (name) advanced to the next position.
                 */
            }
            debugLog("Assumed value: $assumedValue")
        }

        ready.complete(Unit)
        debugLog("Advancing to $destinationValue successful.")
    }

    /**
     * Return a deferred for ready.
     */
    fun waitForReady(): Deferred<Unit> {
        return ready
    }

    /**
     * Set the destination value which this reel should try to reach.
     *
     * @param value Destination value which this reel should try to reach
     */
    fun setDestinationValue(value: Int) {
        if (destinationValue != value) {
            debugLog("Setting new score_reel value. Old destination value: $destinationValue, New destination value: $value")

            destinationValue = value
            busy.complete(Unit)
        }
    }
}
