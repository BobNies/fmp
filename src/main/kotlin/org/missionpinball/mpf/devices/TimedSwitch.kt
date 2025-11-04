package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Timed Switch device.
 *
 * A timed switch monitors one or more switches and posts events when
 * those switches are active (or inactive) for a specified duration.
 *
 * Note: In Python, this inherits from both SystemWideDevice and ModeDevice.
 * In Kotlin, it extends SystemWideDevice and includes mode-related functionality directly.
 *
 * Note: In Python, this uses @DeviceMonitor("active_switches") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class TimedSwitch(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "timed_switches"
    override val collection = "timed_switches"
    override val classLabel = "timed_switch"

    /**
     * Set of currently active switches (switches that have been active for the configured time).
     */
    val activeSwitches = mutableSetOf<String>()

    /**
     * Mode this device is associated with (if in mode).
     */
    var mode: Mode? = null

    /**
     * Whether this device can exist outside of a game.
     */
    val canExistOutsideOfGame: Boolean = true

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): MutableMap<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // Set default event names if not specified
        for (eventType in listOf("active", "released")) {
            val key = "events_when_$eventType"
            if (validatedConfig[key] == null || (validatedConfig[key] as? List<*>)?.isEmpty() == true) {
                validatedConfig[key] = listOf("${name}_$eventType")
            }
        }

        // Convert state string to integer
        when (validatedConfig["state"]) {
            "active" -> validatedConfig["state"] = 1
            "inactive" -> validatedConfig["state"] = 0
        }

        return validatedConfig
    }

    override suspend fun initialize() {
        super.initialize()

        // Add switches from switch tags
        val switchTags = config["switch_tags"] as? List<*> ?: emptyList<String>()
        val switches = (config["switches"] as? MutableList<Any?>) ?: mutableListOf()

        for (tag in switchTags) {
            // TODO: Implement when switch tagging is available
            /*
            for (switch in machine.switches.itemsTagged(tag as String)) {
                if (switch !in switches) {
                    switches.add(switch)
                }
            }
            */
        }

        config["switches"] = switches

        registerSwitchHandlers()
    }

    /**
     * Called when device is removed from a mode.
     *
     * @param mode Mode being removed
     */
    fun deviceRemovedFromMode(mode: Mode) {
        removeSwitchHandlers()
    }

    /**
     * Register switch handlers for all configured switches.
     */
    private fun registerSwitchHandlers() {
        val switches = config["switches"] as? List<*> ?: emptyList<Any>()
        val state = config["state"] as? Int ?: 1
        val time = config["time"] as? Int ?: 0

        for (switchItem in switches) {
            // TODO: Implement when Switch device is fully available with handler support
            /*
            val switch = switchItem as Switch
            switch.addHandler(
                callback = { switchName, switchState, ms ->
                    activate(switchName, switchState, ms)
                },
                state = state xor 0,
                ms = time,
                returnInfo = true
            )
            switch.addHandler(
                callback = { switchName, switchState, ms ->
                    deactivate(switchName, switchState, ms)
                },
                state = state xor 1,
                ms = 0,
                returnInfo = true
            )
            */
        }
    }

    /**
     * Remove switch handlers for all configured switches.
     */
    private fun removeSwitchHandlers() {
        val switches = config["switches"] as? List<*> ?: emptyList<Any>()
        val state = config["state"] as? Int ?: 1

        for (switchItem in switches) {
            // TODO: Implement when Switch device is fully available with handler support
            /*
            val switch = switchItem as Switch
            switch.removeHandler(
                callback = ::activate,
                state = if (state == 1) 1 else 0
            )
            switch.removeHandler(
                callback = ::deactivate,
                state = if (state == 1) 0 else 1
            )
            */
        }
    }

    /**
     * Called when a switch becomes active for the configured time.
     *
     * @param switchName Name of the switch
     * @param state Switch state (not used)
     * @param ms Milliseconds the switch has been in this state (not used)
     */
    private fun activate(switchName: String, state: Int, ms: Int) {
        // Post event only when transitioning from no active switches to some active switches
        if (activeSwitches.isEmpty()) {
            val events = config["events_when_active"] as? List<*> ?: emptyList<String>()
            for (event in events) {
                machine.events.post(event as String)
            }
        }

        activeSwitches.add(switchName)
    }

    /**
     * Called when an active switch is released.
     *
     * @param switchName Name of the switch
     * @param state Switch state (not used)
     * @param ms Milliseconds the switch has been in this state (not used)
     */
    private fun deactivate(switchName: String, state: Int, ms: Int) {
        // Try to remove the switch. Only post event if it was actually active.
        val wasRemoved = activeSwitches.remove(switchName)

        // Post event only when transitioning from some active switches to no active switches
        if (wasRemoved && activeSwitches.isEmpty()) {
            val events = config["events_when_released"] as? List<*> ?: emptyList<String>()
            for (event in events) {
                machine.events.post(event as String)
            }
        }
    }
}

/**
 * Event: (name)_active
 * Config attribute: events_when_active
 *
 * Posted when one of the switches has been active for the configured time.
 */

/**
 * Event: (name)_released
 * Config attribute: events_when_released
 *
 * Posted when one of the switches that has previously been active
 * for more than the configured time has been released.
 */
