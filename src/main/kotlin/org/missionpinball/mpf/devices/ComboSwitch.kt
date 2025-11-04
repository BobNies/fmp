package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Combo Switch device.
 *
 * Tracks combinations of switches being pressed. Can detect when both groups
 * of switches are active at the same time (within a configurable time window),
 * when only one group is active, and when both are inactive.
 *
 * Supports hold times (minimum time switches must be active) and release times
 * (minimum time before considering switches released).
 *
 * Note: In Python, this uses @DeviceMonitor("state") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class ComboSwitch(machine: MachineController, name: String) :
    SystemWideDevice(machine, name), ModeDevice(machine, name) {

    override val configSection = "combo_switches"
    override val collection = "combo_switches"
    override val classLabel = "combo_switch"

    /**
     * Valid states for the combo switch.
     */
    private val states = listOf("inactive", "both", "one")

    /**
     * Current state.
     */
    private var _state = "inactive"

    /**
     * Whether switches_1 are active (timestamp when activated, or null).
     */
    private var switches1Active: Double? = null

    /**
     * Whether switches_2 are active (timestamp when activated, or null).
     */
    private var switches2Active: Double? = null

    /**
     * Delay manager for timing operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Switch handler keys for cleanup.
     */
    private val switchHandlers = mutableListOf<Any>()

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix).toMutableMap()

        // Set default event names if not configured
        for (state in states + listOf("switches_1", "switches_2")) {
            val eventName = "events_when_$state"
            if (validatedConfig[eventName] == null) {
                validatedConfig[eventName] = listOf("${name}_$state")
            }
        }

        return validatedConfig
    }

    override suspend fun deviceAddedSystemWide() {
        super.deviceAddedSystemWide()
        addSwitchHandlers()
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        addSwitchHandlers()
    }

    /**
     * Add switch handlers for tagged and configured switches.
     */
    private fun addSwitchHandlers() {
        // Add switches from tags
        val tag1 = config["tag_1"] as? List<*>
        if (tag1 != null) {
            for (tag in tag1) {
                val tagStr = tag.toString()
                // TODO: Get tagged switches when available
                /*
                for (switch in machine.switches.itemsTagged(tagStr)) {
                    val switches1 = config["switches_1"] as? MutableSet<Switch>
                    switches1?.add(switch)
                }
                */
            }
        }

        val tag2 = config["tag_2"] as? List<*>
        if (tag2 != null) {
            for (tag in tag2) {
                val tagStr = tag.toString()
                // TODO: Get tagged switches when available
                /*
                for (switch in machine.switches.itemsTagged(tagStr)) {
                    val switches2 = config["switches_2"] as? MutableSet<Switch>
                    switches2?.add(switch)
                }
                */
            }
        }

        registerSwitchHandlers()
    }

    /**
     * Current state of the combo switch.
     */
    val state: String
        get() = _state

    override val canExistOutsideOfGame: Boolean
        get() = true

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        removeSwitchHandlers()
        killDelays()
    }

    /**
     * Register switch handlers for all configured switches.
     */
    private fun registerSwitchHandlers() {
        val switches1 = config["switches_1"] as? List<*> ?: emptyList<Switch>()
        for (switch in switches1) {
            val sw = switch as? Switch ?: continue
            // TODO: Register switch handlers when available
            /*
            val handler1 = sw.addHandler(::switch1WentActive, state = 1, returnInfo = true)
            switchHandlers.add(handler1)
            val handler2 = sw.addHandler(::switch1WentInactive, state = 0, returnInfo = true)
            switchHandlers.add(handler2)
            */
        }

        val switches2 = config["switches_2"] as? List<*> ?: emptyList<Switch>()
        for (switch in switches2) {
            val sw = switch as? Switch ?: continue
            // TODO: Register switch handlers when available
            /*
            val handler1 = sw.addHandler(::switch2WentActive, state = 1, returnInfo = true)
            switchHandlers.add(handler1)
            val handler2 = sw.addHandler(::switch2WentInactive, state = 0, returnInfo = true)
            switchHandlers.add(handler2)
            */
        }
    }

    /**
     * Remove all switch handlers.
     */
    private fun removeSwitchHandlers() {
        // TODO: Remove switch handlers when available
        /*
        machine.switchController.removeSwitchHandlerByKeys(switchHandlers)
        */
        switchHandlers.clear()
    }

    /**
     * Clear all delays.
     */
    private fun killDelays() {
        delay.clear()
    }

    /**
     * Handle switches_1 going active.
     */
    private fun switch1WentActive(switchName: String) {
        debugLog("A switch from switches_1 just went active")
        delay.remove("switch_1_inactive")

        if (switches1Active != null) {
            return
        }

        val holdTime = (config["hold_time"] as? Number)?.toLong()
        if (holdTime == null || holdTime == 0L) {
            activateSwitches1(switchName)
        } else {
            delay.addIfDoesntExist(holdTime, "switch_1_active") {
                activateSwitches1(switchName)
            }
        }
    }

    /**
     * Handle switches_2 going active.
     */
    private fun switch2WentActive(switchName: String) {
        debugLog("A switch from switches_2 just went active")
        delay.remove("switch_2_inactive")

        if (switches2Active != null) {
            return
        }

        val holdTime = (config["hold_time"] as? Number)?.toLong()
        if (holdTime == null || holdTime == 0L) {
            activateSwitches2(switchName)
        } else {
            delay.addIfDoesntExist(holdTime, "switch_2_active") {
                activateSwitches2(switchName)
            }
        }
    }

    /**
     * Handle switches_1 going inactive.
     */
    private fun switch1WentInactive(switchName: String) {
        debugLog("A switch from switches_1 just went inactive")

        // Check if at least one switch is still active
        val switches1 = config["switches_1"] as? List<*> ?: emptyList<Switch>()
        for (switch in switches1) {
            val sw = switch as? Switch ?: continue
            if (sw.state) {
                // At least one switch is still active
                return
            }
        }

        delay.remove("switch_1_active")

        val releaseTime = (config["release_time"] as? Number)?.toLong()
        if (releaseTime == null || releaseTime == 0L) {
            releaseSwitches1(switchName)
        } else {
            delay.addIfDoesntExist(releaseTime, "switch_1_inactive") {
                releaseSwitches1(switchName)
            }
        }
    }

    /**
     * Handle switches_2 going inactive.
     */
    private fun switch2WentInactive(switchName: String) {
        debugLog("A switch from switches_2 just went inactive")

        // Check if at least one switch is still active
        val switches2 = config["switches_2"] as? List<*> ?: emptyList<Switch>()
        for (switch in switches2) {
            val sw = switch as? Switch ?: continue
            if (sw.state) {
                // At least one switch is still active
                return
            }
        }

        delay.remove("switch_2_active")

        val releaseTime = (config["release_time"] as? Number)?.toLong()
        if (releaseTime == null || releaseTime == 0L) {
            releaseSwitches2(switchName)
        } else {
            delay.addIfDoesntExist(releaseTime, "switch_2_inactive") {
                releaseSwitches2(switchName)
            }
        }
    }

    /**
     * Activate switches_1 after hold time has passed.
     */
    private fun activateSwitches1(switchName: String) {
        debugLog("Switches_1 has passed the hold time and is now active")
        switches1Active = machine.clock.getTime()
        delay.remove("switch_2_only")

        val switches2ActiveTime = switches2Active
        if (switches2ActiveTime != null) {
            val maxOffsetTime = (config["max_offset_time"] as? Number)?.toDouble() ?: -1.0
            if (maxOffsetTime >= 0 &&
                (switches1Active!! - switches2ActiveTime > maxOffsetTime)) {
                debugLog("Switches_2 is active, but the max_offset_time=$maxOffsetTime " +
                    "is larger than when a Switches_2 switch was first activated, so " +
                    "the state will not switch to 'both'")
                return
            }

            switchState("both", 1, switchName)
        } else {
            val maxOffsetTime = (config["max_offset_time"] as? Number)?.toLong() ?: -1L
            if (maxOffsetTime >= 0) {
                delay.addIfDoesntExist(maxOffsetTime * 1000, "switch_1_only") {
                    postOnlyOneActiveEvent(1)
                }
            }
        }
    }

    /**
     * Activate switches_2 after hold time has passed.
     */
    private fun activateSwitches2(switchName: String) {
        debugLog("Switches_2 has passed the hold time and is now active")
        switches2Active = machine.clock.getTime()
        delay.remove("switch_1_only")

        val switches1ActiveTime = switches1Active
        if (switches1ActiveTime != null) {
            val maxOffsetTime = (config["max_offset_time"] as? Number)?.toDouble() ?: -1.0
            if (maxOffsetTime >= 0 &&
                (switches2Active!! - switches1ActiveTime > maxOffsetTime)) {
                debugLog("Switches_1 is active, but the max_offset_time=$maxOffsetTime " +
                    "is larger than when a Switches_1 switch was first activated, so " +
                    "the state will not switch to 'both'")
                return
            }

            switchState("both", 2, switchName)
        } else {
            val maxOffsetTime = (config["max_offset_time"] as? Number)?.toLong() ?: -1L
            if (maxOffsetTime >= 0) {
                delay.addIfDoesntExist(maxOffsetTime * 1000, "switch_2_only") {
                    postOnlyOneActiveEvent(2)
                }
            }
        }
    }

    /**
     * Post event when only one group is active after max_offset_time.
     */
    private fun postOnlyOneActiveEvent(number: Int) {
        val events = config["events_when_switches_$number"] as? List<*> ?: emptyList<String>()
        for (event in events) {
            machine.events.post(event.toString())
        }
    }

    /**
     * Release switches_1 after release time has passed.
     */
    private fun releaseSwitches1(switchName: String) {
        debugLog("Switches_1 has passed the release time and is now released")
        switches1Active = null

        if (switches2Active != null && _state == "both") {
            switchState("one", 1, switchName)
        } else if (_state == "one") {
            switchState("inactive", 1, switchName)
        }
    }

    /**
     * Release switches_2 after release time has passed.
     */
    private fun releaseSwitches2(switchName: String) {
        debugLog("Switches_2 has passed the release time and is now released")
        switches2Active = null

        if (switches1Active != null && _state == "both") {
            switchState("one", 2, switchName)
        } else if (_state == "one") {
            switchState("inactive", 2, switchName)
        }
    }

    /**
     * Switch to a new state and post events.
     *
     * @param state New state to switch to
     * @param group Group number that triggered the state change
     * @param switch Switch name that triggered the state change
     */
    private fun switchState(state: String, group: Int, switch: String) {
        if (state !in states) {
            throw IllegalArgumentException("Received invalid state: $state")
        }

        if (state == _state) {
            return
        }

        _state = state
        debugLog("New State: $state")

        val events = config["events_when_$state"] as? List<*> ?: emptyList<String>()
        for (event in events) {
            machine.events.post(
                event.toString(),
                mapOf(
                    "triggering_group" to group,
                    "triggering_switch" to switch
                )
            )
        }

        /**
         * Event: (name)_one
         * Config attribute: events_when_one
         *
         * Combo switch (name) changed to state one.
         *
         * Either switch 1 or switch 2 has been released for at
         * least the release_time but the other switch is still active.
         */

        /**
         * Event: (name)_both
         * Config attribute: events_when_both
         *
         * Combo switch (name) changed to state both.
         *
         * A switch from group 1 and group 2 are both active at the
         * same time, having been pressed within the max_offset_time and
         * being active for at least the hold_time.
         */

        /**
         * Event: (name)_inactive
         * Config attribute: events_when_inactive
         *
         * Combo switch (name) changed to state inactive.
         *
         * Both switches are inactive.
         */

        /**
         * Event: (name)_switches_1
         * Config attribute: events_when_switches_1
         *
         * Combo switch (name) changed to state switches_1.
         *
         * Only switches_1 is active. max_offset_time has passed and this hit
         * cannot become both later on. Only emitted when max_offset_time is defined.
         */

        /**
         * Event: (name)_switches_2
         * Config attribute: events_when_switches_2
         *
         * Combo switch (name) changed to state switches_2.
         *
         * Only switches_2 is active. max_offset_time has passed and this hit
         * cannot become both later on. Only emitted when max_offset_time is defined.
         */
    }
}
