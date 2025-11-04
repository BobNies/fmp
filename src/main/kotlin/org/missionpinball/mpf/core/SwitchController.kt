package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.missionpinball.mpf.devices.Switch

private val logger = KotlinLogging.logger {}

/**
 * Monitored switch change data.
 */
data class MonitoredSwitchChange(
    val name: String,
    val label: String,
    val platform: Any?,
    val num: String,
    val state: Int
)

/**
 * Switch handler registration data.
 */
data class SwitchHandler(
    val switchName: String,
    val callback: (Map<String, Any?>) -> Unit,
    val state: Int,
    val ms: Long
)

/**
 * Timed switch handler data.
 */
data class TimedSwitchHandler(
    val callback: (Map<String, Any?>) -> Unit,
    val state: Int,
    val ms: Long
)

/**
 * Registered switch handler.
 */
class RegisteredSwitch(
    val ms: Long,
    val callback: (Map<String, Any?>) -> Unit
) {
    var cancelled = false
}

/**
 * Switch controller.
 *
 * Tracks all switches in the machine, receives switch activity,
 * and converts switch changes into events.
 */
class SwitchController(machine: MachineController) : MpfController(machine) {

    override val configName = "switch_controller"

    /**
     * Dictionary of switches and states that have been registered for callbacks.
     */
    private val registeredSwitches = mutableMapOf<Switch, List<MutableList<RegisteredSwitch>>>()

    /**
     * Delay handlers for timed switches.
     */
    private val timedSwitchHandlerDelay = mutableMapOf<String, Any>()

    /**
     * Dictionary of switches currently in a state counting ms.
     */
    private val activeTimed Switches = mutableMapOf<String, MutableMap<Double, MutableList<TimedSwitchHandler>>>()

    /**
     * Lookup table for switch + platform to Switch object.
     */
    private val switchLookup = mutableMapOf<Pair<Any?, Any?>, Switch>()

    /**
     * Monitors for switch changes.
     */
    val monitors = mutableListOf<(MonitoredSwitchChange) -> Unit>()

    /**
     * Whether switches have been initialized.
     */
    private var initialized = false

    init {
        // TODO: Register event handlers when event system is fully integrated
        // machine.events.addAsyncHandler("init_phase_2", ::initializeSwitches, priority = 1000)
    }

    /**
     * Register a switch for tracking.
     *
     * @param switch Switch object to register.
     */
    fun registerSwitch(switch: Switch) {
        registeredSwitches[switch] = listOf(mutableListOf(), mutableListOf())
    }

    /**
     * Initialize switches from hardware.
     */
    private suspend fun initializeSwitches() {
        updateSwitchesFromHw()

        // TODO: Build lookup table when switch collection is available
        /*
        for (switch in machine.switches.values) {
            val key = Pair(switch.hwSwitch?.number, switch.hwSwitch?.platform)
            switchLookup[key] = switch
        }
        */

        initialized = true
        logActiveSwitches()
    }

    /**
     * Update switch states from hardware.
     *
     * This method works silently and does not post any events if switches change state.
     */
    suspend fun updateSwitchesFromHw() {
        // TODO: Implement when platform system is available
        /*
        val platforms = mutableSetOf<Any>()
        val switches = mutableSetOf<Pair<Switch, Any>>()

        for (switch in machine.switches.values) {
            platforms.add(switch.platform)
            switches.add(Pair(switch, switch.hwSwitch.number))
        }

        for (platform in platforms) {
            val switchStates = platform.getHwSwitchStates()

            for ((switch, number) in switches) {
                if (switch.platform != platform) continue

                try {
                    switch.state = switchStates[number] xor switch.invert
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Missing switch $switch in update from HW: $platform\n" +
                        "HW States: $switchStates\nKnown switches: $switches", e
                    )
                }
            }
        }
        */
    }

    /**
     * Verify that switch states match hardware.
     *
     * @return True if all states match, false otherwise.
     */
    suspend fun verifySwitches(): Boolean {
        // TODO: Implement when switch collection is available
        /*
        val currentStates = mutableMapOf<Switch, Int>()

        for (switch in machine.switches.values) {
            currentStates[switch] = switch.state
        }

        updateSwitchesFromHw()

        var ok = true
        for (switch in machine.switches.values) {
            if (switch.state != currentStates[switch]) {
                ok = false
                warningLog(
                    "Switch State Error! Switch: %s, HW State: %s, MPF State: %s",
                    switch.name, currentStates[switch], switch.state
                )
            }
        }

        return ok
        */
        return true
    }

    /**
     * Check if a switch is in a given state.
     *
     * @param switch Switch to check.
     * @param state State to check (0 = inactive, 1 = active).
     * @param ms Optional: milliseconds switch must be in state.
     * @return True if switch is in state (for at least ms if specified).
     */
    fun isState(switch: Switch, state: Int, ms: Long = 0): Boolean {
        check(initialized) { "Cannot read switch state before init_phase_3" }

        return if (ms > 0) {
            switch.state == state && ms <= switch.getMsSinceLastChange()
        } else {
            switch.state == state
        }
    }

    /**
     * Check if a switch is active.
     *
     * @param switch Switch to check.
     * @param ms Optional: milliseconds switch must be active.
     * @return True if switch is active (for at least ms if specified).
     */
    fun isActive(switch: Switch, ms: Long = 0): Boolean {
        check(initialized) { "Cannot read switch state before init_phase_3" }

        return if (ms > 0) {
            switch.state == 1 && ms <= switch.getMsSinceLastChange()
        } else {
            switch.state == 1
        }
    }

    /**
     * Check if a switch is inactive.
     *
     * @param switch Switch to check.
     * @param ms Optional: milliseconds switch must be inactive.
     * @return True if switch is inactive (for at least ms if specified).
     */
    fun isInactive(switch: Switch, ms: Long = 0): Boolean {
        check(initialized) { "Cannot read switch state before init_phase_3" }

        return if (ms > 0) {
            switch.state == 0 && ms <= switch.getMsSinceLastChange()
        } else {
            switch.state == 0
        }
    }

    /**
     * Process a switch state change by switch number.
     *
     * @param num Switch number (platform-specific).
     * @param state New state (0 or 1).
     * @param platform Platform this switch is on.
     * @param logical Whether state is logical (vs physical).
     * @param timestamp Timestamp of the change.
     */
    fun processSwitchByNum(
        num: Any,
        state: Int,
        platform: Any,
        logical: Boolean = false,
        timestamp: Long? = null
    ) {
        check(initialized) {
            "Got early switch change for switch $num to state $state. platform: $platform"
        }

        val switch = switchLookup[Pair(num, platform)]

        if (switch != null) {
            processSwitchObj(switch, state, logical, timestamp)
        } else {
            if (debug) {
                debugLog("Unknown switch %s change to state %s on platform %s", num, state, platform)
            }

            // Trigger monitors for unknown switch
            for (monitor in monitors) {
                monitor(
                    MonitoredSwitchChange(
                        name = num.toString(),
                        label = "$platform-$num",
                        platform = platform,
                        num = num.toString(),
                        state = state
                    )
                )
            }
        }
    }

    /**
     * Process a switch state change by name.
     *
     * @param name Switch name.
     * @param state New state (0 or 1).
     * @param logical Whether state is logical (vs physical).
     * @param timestamp Timestamp of the change.
     */
    fun processSwitch(
        name: String,
        state: Int,
        logical: Boolean = false,
        timestamp: Long? = null
    ) {
        if (debug) {
            debugLog("Processing switch. Name: %s, state: %s, logical: %s", name, state, logical)
        }

        val time = timestamp ?: machine.clock.getTime()

        // TODO: Get switch from collection when available
        /*
        val switch = machine.switches[name] ?: throw IllegalArgumentException(
            "Cannot process switch \"$name\" as this is not a valid switch name."
        )

        processSwitchObj(switch, state, logical, time)
        */
    }

    /**
     * Process a switch state change for a switch object.
     *
     * @param obj Switch object.
     * @param state New state (boolean or int: true/1 = active, false/0 = inactive).
     * @param logical Whether state is logical (vs physical).
     * @param timestamp Timestamp of the change.
     */
    fun processSwitchObj(
        obj: Switch,
        state: Int,
        logical: Boolean,
        timestamp: Long? = null
    ) {
        require(obj.hwSwitch != null) { "Switch ${obj.name} has no hardware switch" }

        // Normalize state to 0 or 1
        val normalizedState = if (state != 0) 1 else 0
        val time = timestamp ?: machine.clock.getTime()

        // Flip logical & physical states for NC switches
        var hwState = normalizedState

        if (obj.invert != 0) {
            hwState = if (logical) {
                // NC + logical means hw_state is opposite of state
                1 - normalizedState
            } else {
                // NC + physical means state is opposite of hw_state
                normalizedState
            }
        }

        // Set physical state
        obj.hwState = hwState

        // Calculate logical state
        obj.state = hwState xor obj.invert

        // Update last change time
        // TODO: Update when we have proper time tracking
        // obj.lastChange = time

        // Post events and call handlers
        if (!obj.isMuted) {
            processHandlers(obj, obj.state)
        }

        // Notify monitors
        for (monitor in monitors) {
            monitor(
                MonitoredSwitchChange(
                    name = obj.name,
                    label = obj.label ?: obj.name,
                    platform = obj.platform,
                    num = obj.hwSwitch?.toString() ?: "unknown",
                    state = obj.state
                )
            )
        }
    }

    /**
     * Process handlers for a switch state change.
     */
    private fun processHandlers(switch: Switch, state: Int) {
        // TODO: Implement handler processing when fully integrated
        debugLog("Processing handlers for switch ${switch.name}, state: $state")
    }

    /**
     * Add a switch handler.
     *
     * @param switch Switch object.
     * @param callback Callback to call on state change.
     * @param state State to trigger on (0 or 1).
     * @param ms Milliseconds switch must be in state.
     * @param returnInfo Whether to pass switch info to callback.
     * @param callbackKwargs Additional kwargs for callback.
     * @return Handler key.
     */
    fun addSwitchHandlerObj(
        switch: Switch,
        callback: (Map<String, Any?>) -> Unit,
        state: Int = 1,
        ms: Long = 0,
        returnInfo: Boolean = false,
        callbackKwargs: Map<String, Any?>? = null
    ): Any {
        // TODO: Implement full handler registration
        debugLog("Adding switch handler for ${switch.name}, state: $state, ms: $ms")
        return Any()
    }

    /**
     * Remove a switch handler.
     *
     * @param switch Switch object.
     * @param callback Callback to remove.
     * @param state State the handler was registered for.
     * @param ms Ms delay the handler was registered for.
     */
    fun removeSwitchHandlerObj(
        switch: Switch,
        callback: (Map<String, Any?>) -> Unit,
        state: Int = 1,
        ms: Long = 0
    ) {
        // TODO: Implement handler removal
        debugLog("Removing switch handler for ${switch.name}, state: $state, ms: $ms")
    }

    /**
     * Log all active switches.
     */
    private fun logActiveSwitches() {
        // TODO: Implement when switch collection is available
        /*
        val activeSwitches = machine.switches.values.filter { it.state == 1 }
        if (activeSwitches.isNotEmpty()) {
            infoLog("Active switches: ${activeSwitches.joinToString { it.name }}")
        } else {
            infoLog("No active switches")
        }
        */
    }
}
