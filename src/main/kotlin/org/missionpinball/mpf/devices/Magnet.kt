package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Controls a playfield magnet in a pinball machine.
 *
 * Magnets can grab, hold, and release balls. They can also "fling" balls
 * by quickly disabling and re-enabling the magnet to impart momentum.
 *
 * Note: In Python, this uses EnableDisableMixinSystemWideDevice for
 * enable/disable functionality. In Kotlin, this is implemented directly.
 *
 * Note: In Python, this uses @DeviceMonitor("_active", "_release_in_progress") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Magnet(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "magnets"
    override val collection = "magnets"
    override val classLabel = "magnet"

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Playfield this magnet is on.
     */
    var playfield: Any? = null

    /**
     * Whether the magnet is currently active (holding a ball).
     */
    private var _active = false

    /**
     * Whether a release is currently in progress.
     */
    private var _releaseInProgress = false

    /**
     * Whether the magnet is enabled.
     */
    var enabled = false
        private set

    /**
     * Event handler for enable event.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable the magnet.
     *
     * When enabled, the magnet will grab balls that activate the grab switch.
     */
    fun enable() {
        if (enabled) {
            return
        }
        enabled = true
        _enable()
    }

    /**
     * Internal enable logic.
     */
    private fun _enable() {
        debugLog("Enabling Magnet")

        val grabSwitch = config["grab_switch"] as? Switch
        if (grabSwitch != null) {
            // TODO: Add switch handler when switch system is fully available
            /*
            grabSwitch.addHandler(::grabBall)
            */
            playfield = config["playfield"] ?: grabSwitch.playfield
        } else {
            playfield = config["playfield"]
        }
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(0) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    /**
     * Disable the magnet.
     *
     * When disabled, the magnet will not grab balls.
     */
    fun disable() {
        if (!enabled) {
            return
        }
        enabled = false
        _disable()
    }

    /**
     * Internal disable logic.
     */
    private fun _disable() {
        debugLog("Disabling Magnet")

        val grabSwitch = config["grab_switch"] as? Switch
        if (grabSwitch != null) {
            // TODO: Remove switch handler when switch system is fully available
            /*
            grabSwitch.removeHandler(::grabBall)
            */
        }
    }

    /**
     * Event handler for reset event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventReset() {
        reset()
    }

    /**
     * Release ball and disable magnet.
     */
    fun reset() {
        debugLog("Resetting Magnet")
        releaseBall()
        disable()
    }

    /**
     * Event handler for grab_ball event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventGrabBall() {
        grabBall()
    }

    /**
     * Grab a ball.
     *
     * Activates the magnet coil to grab a ball. Posts events when
     * the grabbing process starts and completes.
     */
    fun grabBall() {
        // Mark playfield active if available
        // TODO: Implement when playfield is available
        /*
        playfield?.markPlayfieldActiveFromDeviceAction(name)
        */

        // Check if magnet is enabled or already active
        if (!enabled || _active || _releaseInProgress) {
            return
        }

        debugLog("Grabbing a ball.")
        _active = true

        val magnetCoil = config["magnet_coil"] as? Driver
        magnetCoil?.enable()

        machine.events.post(
            "magnet_${name}_grabbing_ball"
        )

        val grabTime = (config["grab_time"] as? Number)?.toLong() ?: 1500L
        delay.add(grabTime, "grabbing_done") {
            _grabbingDone()
        }
    }

    /**
     * Called when the grab time has elapsed.
     */
    private fun _grabbingDone() {
        machine.events.post("magnet_${name}_grabbed_ball")
    }

    /**
     * Event handler for release_ball event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventReleaseBall() {
        releaseBall()
    }

    /**
     * Release the grabbed ball.
     *
     * Disables the magnet coil to release the ball. Posts events when
     * the release process starts and completes.
     */
    fun releaseBall() {
        if (!_active || _releaseInProgress) {
            return
        }

        _active = false
        _releaseInProgress = true
        debugLog("Releasing ball.")

        machine.events.post("magnet_${name}_releasing_ball")

        val releaseTime = (config["release_time"] as? Number)?.toLong() ?: 500L
        delay.add(releaseTime, "release_done") {
            _releaseDone()
        }

        val magnetCoil = config["magnet_coil"] as? Driver
        magnetCoil?.disable()
    }

    /**
     * Called when the release time has elapsed.
     */
    private fun _releaseDone() {
        _releaseInProgress = false
        machine.events.post("magnet_${name}_released_ball")
    }

    /**
     * Event handler for fling_ball event.
     *
     * TODO: Add @EventHandler(7) annotation when event system is fully integrated
     */
    fun eventFlingBall() {
        flingBall()
    }

    /**
     * Fling the grabbed ball.
     *
     * Quickly disables and re-enables the magnet to impart momentum to
     * the ball, effectively "throwing" it. Posts events when the fling
     * process starts and completes.
     */
    fun flingBall() {
        if (!_active || _releaseInProgress) {
            return
        }

        _active = false
        _releaseInProgress = true
        debugLog("Flinging ball.")

        machine.events.post("magnet_${name}_flinging_ball")

        val flingDropTime = (config["fling_drop_time"] as? Number)?.toLong() ?: 250L
        delay.add(flingDropTime, "fling_reenable") {
            _flingReenable()
        }

        val magnetCoil = config["magnet_coil"] as? Driver
        magnetCoil?.disable()
    }

    /**
     * Re-enable magnet during fling sequence.
     */
    private fun _flingReenable() {
        val flingRegrabTime = (config["fling_regrab_time"] as? Number)?.toLong() ?: 50L
        delay.add(flingRegrabTime, "fling_done") {
            _flingDone()
        }

        val magnetCoil = config["magnet_coil"] as? Driver
        magnetCoil?.enable()
    }

    /**
     * Called when the fling sequence is complete.
     */
    private fun _flingDone() {
        _releaseInProgress = false

        val magnetCoil = config["magnet_coil"] as? Driver
        magnetCoil?.disable()

        machine.events.post("magnet_${name}_flinged_ball")
    }
}

/**
 * Event: magnet_(name)_grabbing_ball
 *
 * The magnet called (name) is attempting to grab a ball.
 */

/**
 * Event: magnet_(name)_grabbed_ball
 *
 * The magnet called (name) has completed grabbing the ball.
 * Note that the magnet doesn't actually "know" whether it
 * successfully grabbed a ball or not, so this event is saying that it
 * thinks it did.
 */

/**
 * Event: magnet_(name)_releasing_ball
 *
 * The magnet called (name) is in the process of releasing a ball.
 */

/**
 * Event: magnet_(name)_released_ball
 *
 * The magnet called (name) has just released a ball.
 */

/**
 * Event: magnet_(name)_flinging_ball
 *
 * The magnet called (name) is flinging a ball by disabling and
 * enabling the magnet again for a short time.
 */

/**
 * Event: magnet_(name)_flinged_ball
 *
 * The magnet called (name) has just flinged a ball.
 */
