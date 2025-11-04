package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Autofire coils which fire based on switch hits with a hardware rule.
 *
 * Coils in the pinball machine which should fire automatically based on
 * switch hits using defined hardware switch rules.
 *
 * Autofire coils work with rules written to the hardware pinball controller
 * that allow them to respond "instantly" to switch hits versus waiting for
 * the lag of USB and the host computer.
 *
 * Examples of Autofire Coils are pop bumpers, slingshots, and kicking
 * targets. (Flippers use the same autofire rules under the hood, but flipper
 * devices have their own device type in MPF.)
 *
 * Note: In Python, this uses @DeviceMonitor("_enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class AutofireCoil(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "autofire_coils"
    override val collection = "autofire_coils"
    override val classLabel = "autofire"

    /**
     * Whether the autofire is enabled.
     */
    private var _enabled = false

    /**
     * The hardware rule object (when platform controller is available).
     */
    private var rule: Any? = null

    /**
     * Delay manager for timeout handling.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Whether ball search is currently in progress.
     */
    private var ballSearchInProgress = false

    /**
     * Playfield this autofire is on.
     */
    var playfield: Any? = null

    /**
     * Watch time for timeout detection (in seconds).
     */
    private var timeoutWatchTime: Double? = null

    /**
     * Maximum hits within watch time before disabling.
     */
    private var timeoutMaxHits: Int? = null

    /**
     * Time to disable after timeout (in milliseconds).
     */
    private var timeoutDisableTime: Long? = null

    /**
     * List of hit timestamps for timeout detection.
     */
    private val timeoutHits = mutableListOf<Double>()

    override suspend fun initialize() {
        super.initialize()

        val switchConfig = config["switch"] as? Switch
        playfield = config["playfield"] ?: switchConfig?.playfield

        val ballSearchOrder = config["ball_search_order"] as? Int
        if (ballSearchOrder != null) {
            // TODO: Register with ball search when available
            /*
            playfield?.ballSearch?.register(ballSearchOrder, ::ballSearch, name)
            */
        }

        // Pulse is handled via rule but add a handler so that we take notice anyway
        // TODO: Add switch handler when switch system is fully available
        /*
        switchConfig?.addHandler(::hit)
        */

        val timeoutWatchTimeMs = config["timeout_watch_time"] as? Int
        if (timeoutWatchTimeMs != null) {
            timeoutWatchTime = timeoutWatchTimeMs / 1000.0
            timeoutMaxHits = config["timeout_max_hits"] as? Int
            timeoutDisableTime = (config["timeout_disable_time"] as? Number)?.toLong()
        }

        // Validate that switch doesn't have redundant playfield active tag
        // TODO: Implement when switch tags are available
        /*
        val playfieldActiveName = "${playfield?.name}_active"
        if (switchConfig?.tags?.contains(playfieldActiveName) == true) {
            raiseConfigError(
                "Autofire device '$name' uses switch '${switchConfig.name}' which has a " +
                "'${playfieldActiveName}' tag. This is handled internally by the device. Remove the " +
                "redundant '${playfieldActiveName}' tag from that switch.", 1
            )
        }
        */
    }

    /**
     * Event handler for enable event.
     *
     * To prevent multiple rules at the same time we prioritize disable > enable.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable the autofire device.
     *
     * This causes the coil to respond to the switch hits. This is typically
     * called when a ball starts to enable the slingshots, pops, etc.
     *
     * Note that there are several options for both the coil and the switch
     * which can be incorporated into this rule, including recycle times,
     * switch debounce, reversing the switch (fire the coil when the switch
     * goes inactive), etc. These rules vary by hardware platform.
     */
    fun enable() {
        if (_enabled) {
            return
        }
        _enabled = true

        debugLog("Enabling")

        // Determine recycle setting
        val coilOverwrite = config["coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        val coil = config["coil"] as? Driver

        val recycle = if (coilOverwrite["recycle"] != null) {
            // If coil_overwrite is set use it
            coilOverwrite["recycle"] as Boolean
        } else {
            // Otherwise load the default from the coil and turn null to true
            val defaultRecycle = coil?.config?.get("default_recycle")
            defaultRecycle == true || defaultRecycle == null
        }

        // Determine debounce setting
        val switchOverwrite = config["switch_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        val switch = config["switch"] as? Switch

        val debounce = if (switchOverwrite["debounce"] != null) {
            // If switch_overwrite is set use it
            switchOverwrite["debounce"] == "normal"
        } else {
            // Otherwise load the default from the switch and turn auto into false
            switch?.config?.get("debounce") == "normal"
        }

        // Set up hardware rule
        // TODO: Implement when platform controller is available
        /*
        val coilPulseDelay = config["coil_pulse_delay"] as? Int
        if (coilPulseDelay == null || coilPulseDelay == 0) {
            rule = machine.platformController.setPulseOnHitRule(
                SwitchRuleSettings(
                    switch = switch,
                    debounce = debounce,
                    invert = config["reverse_switch"] as? Boolean ?: false
                ),
                DriverRuleSettings(
                    driver = coil,
                    recycle = recycle
                ),
                PulseRuleSettings(
                    duration = coilOverwrite["pulse_ms"] as? Int,
                    power = coilOverwrite["pulse_power"] as? Double
                )
            )
        } else {
            rule = machine.platformController.setDelayedPulseOnHitRule(
                SwitchRuleSettings(
                    switch = switch,
                    debounce = debounce,
                    invert = config["reverse_switch"] as? Boolean ?: false
                ),
                DriverRuleSettings(
                    driver = coil,
                    recycle = recycle
                ),
                coilPulseDelay,
                PulseRuleSettings(
                    duration = coilOverwrite["pulse_ms"] as? Int,
                    power = coilOverwrite["pulse_power"] as? Double
                )
            )
        }
        */
    }

    /**
     * Event handler for disable event.
     *
     * To prevent multiple rules at the same time we prioritize disable > enable.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    /**
     * Disable the autofire device.
     *
     * This is typically called at the end of a ball and when a tilt event
     * happens.
     */
    fun disable() {
        delay.remove("_timeout_enable_delay")

        if (!_enabled) {
            return
        }
        _enabled = false

        debugLog("Disabling")

        // TODO: Clear hardware rule when platform controller is available
        /*
        machine.platformController.clearHwRule(rule)
        */
    }

    /**
     * Called when the hardware rule was triggered.
     */
    private fun hit() {
        if (!_enabled) {
            return
        }

        if (!ballSearchInProgress) {
            // TODO: Mark playfield active when available
            /*
            playfield?.markPlayfieldActiveFromDeviceAction(name)
            */
        }

        // Handle timeout detection
        if (timeoutWatchTime != null) {
            val currentTime = machine.clock.getTime()

            // Remove old hits outside the watch window
            timeoutHits.removeAll { it <= currentTime - timeoutWatchTime!! }

            // Add current hit
            timeoutHits.add(currentTime)

            // Check if we've exceeded the max hits
            if (timeoutHits.size >= (timeoutMaxHits ?: Int.MAX_VALUE)) {
                disable()
                delay.add(timeoutDisableTime ?: 0L, "_timeout_enable_delay") {
                    enable()
                }
            }
        }
    }

    /**
     * Ball search callback.
     *
     * @param phase Ball search phase
     * @param iteration Ball search iteration
     * @return True if this device can help with ball search
     */
    private fun ballSearch(phase: Int, iteration: Int): Boolean {
        delay.reset(200L, "ball_search_ignore_done") {
            ballSearchIgnoreDone()
        }
        ballSearchInProgress = true

        val coil = config["coil"] as? Driver
        coil?.pulse()

        return true
    }

    /**
     * We no longer expect any fake hits from ball search.
     */
    private fun ballSearchIgnoreDone() {
        ballSearchInProgress = false
    }
}
