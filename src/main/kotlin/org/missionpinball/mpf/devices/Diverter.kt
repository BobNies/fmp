package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import java.util.*

/**
 * Represents a diverter in a pinball machine.
 *
 * Diverters are devices that route balls to different targets. They can be
 * activated manually, by switches, or automatically based on ball device ejects.
 * They support both pulse and hold coil types.
 *
 * Note: In Python, this uses @DeviceMonitor("active", "enabled", "eject_state") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Diverter(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "diverters"
    override val collection = "diverters"
    override val classLabel = "diverter"

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Whether the diverter is physically active (coil activated).
     */
    var active = false

    /**
     * Whether the diverter is enabled (will respond to events).
     */
    var enabled = false

    /**
     * Count of diverting ejects in progress.
     */
    private var divertingEjectsCount = 0

    /**
     * Current eject state (true for active targets, false for inactive targets).
     */
    var ejectState = false

    /**
     * Queue of eject attempts waiting for diverter to be available.
     */
    private val ejectAttemptQueue = ArrayDeque<Any>()

    override suspend fun initialize() {
        super.initialize()

        // Register for feeder device eject events
        val feederDevices = config["feeder_devices"] as? List<*> ?: emptyList<Any>()
        for (feederDevice in feederDevices) {
            val deviceName = (feederDevice as? Any)?.toString() ?: continue

            // TODO: Register event handlers when event system is available
            /*
            machine.events.addHandler("balldevice_${deviceName}_ball_eject_attempt", ::feederEjectAttempt)
            machine.events.addHandler("balldevice_${deviceName}_ejecting_ball", ::feederEjecting)
            machine.events.addHandler("balldevice_${deviceName}_ball_eject_failed", ::feederEjectCountDecrease)
            machine.events.addHandler("balldevice_${deviceName}_ball_eject_success", ::feederEjectCountDecrease)
            */
        }

        // TODO: Register switch handlers when available
        /*
        machine.events.addHandler("init_phase_3", ::registerSwitches)
        */

        val ballSearchOrder = config["ball_search_order"] as? Int
        if (ballSearchOrder != null) {
            // TODO: Register ball search when available
            /*
            val playfield = config["playfield"]
            playfield?.ballSearch?.register(
                ballSearchOrder, ::ballSearch, name, restoreCallback = ::ballSearchRestore
            )
            */
        }
    }

    /**
     * Register switch handlers.
     */
    private fun registerSwitches() {
        // Register for deactivation switches
        val deactivationSwitches = config["deactivation_switches"] as? List<*> ?: emptyList<Any>()
        for (switch in deactivationSwitches) {
            // TODO: Register switch handler when available
            /*
            machine.switchController.addSwitchHandlerObj(switch, ::deactivate)
            */
        }

        // Register for disable switches
        val disableSwitches = config["disable_switches"] as? List<*> ?: emptyList<Any>()
        for (switch in disableSwitches) {
            // TODO: Register switch handler when available
            /*
            machine.switchController.addSwitchHandlerObj(switch, ::disable)
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
     * Reset and deactivate the diverter.
     */
    fun reset() {
        deactivate()
    }

    /**
     * Event handler for enable event.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     *
     * @param auto Whether this is an automatic enable
     */
    fun eventEnable(auto: Boolean = false) {
        enable(auto)
    }

    /**
     * Enable this diverter.
     *
     * If an 'activation_switches' is configured, then this method writes a
     * hardware autofire rule to the pinball controller which fires the
     * diverter coil when the switch is activated.
     *
     * If no `activation_switches` is specified, then the diverter is activated
     * immediately.
     *
     * @param auto Boolean value which is used to indicate whether this
     *             diverter enabled itself automatically. This is passed to the
     *             event which is posted.
     */
    fun enable(auto: Boolean = false) {
        if (enabled) {
            return
        }
        enabled = true

        machine.events.post("diverter_${name}_enabling", mapOf("auto" to auto))

        val activationSwitches = config["activation_switches"] as? List<*>
        val activateEvents = config["activate_events"] as? List<*>

        when {
            !activationSwitches.isNullOrEmpty() -> enableSwitches()
            !activateEvents.isNullOrEmpty() -> {
                // Will be activated by event handlers - nothing to do here
            }
            else -> activate()
        }
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(0) annotation when event system is fully integrated
     *
     * @param auto Whether this is an automatic disable
     */
    fun eventDisable(auto: Boolean = false) {
        disable(auto)
    }

    /**
     * Disable this diverter.
     *
     * This method will remove the hardware rule if this diverter is activated
     * via a hardware switch.
     *
     * @param auto Boolean value which is used to indicate whether this
     *             diverter disabled itself automatically. This is passed to the
     *             event which is posted.
     */
    fun disable(auto: Boolean = false) {
        if (!enabled) {
            return
        }
        enabled = false

        machine.events.post("diverter_${name}_disabling", mapOf("auto" to auto))

        debugLog("Disabling Diverter")

        val activationSwitches = config["activation_switches"] as? List<*>
        if (!activationSwitches.isNullOrEmpty()) {
            disableSwitches()
        }

        // If there is no deactivation way
        val activationTime = config["activation_time"] as? Long
        val deactivationSwitches = config["deactivation_switches"] as? List<*>
        val deactivateEvents = config["deactivate_events"] as? List<*>

        if (activationTime == null &&
            deactivationSwitches.isNullOrEmpty() &&
            deactivateEvents.isNullOrEmpty()) {
            deactivate()
        }
    }

    /**
     * Activate the coil.
     */
    private fun coilActivate() {
        val activationCoil = config["activation_coil"] as? Driver
        val type = config["type"] as? String

        if (activationCoil != null) {
            when (type) {
                "pulse" -> {
                    debugLog("Pulsing coil to activate diverter")
                    activationCoil.pulse()
                }
                "hold" -> {
                    debugLog("Enabling coil to activate diverter")
                    activationCoil.enable()
                }
            }
        }
    }

    /**
     * Deactivate the coil.
     */
    private fun coilDeactivate() {
        val activationCoil = config["activation_coil"] as? Driver
        if (activationCoil != null) {
            debugLog("Disabling coil to deactivate diverter")
            activationCoil.disable()
        }

        val deactivationCoil = config["deactivation_coil"] as? Driver
        if (deactivationCoil != null) {
            debugLog("Pulsing coil to deactivate diverter")
            deactivationCoil.pulse()
        }
    }

    /**
     * Event handler for activate event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventActivate() {
        if (!enabled) {
            return
        }
        activate()
    }

    /**
     * Physically activate this diverter's coil.
     */
    fun activate() {
        debugLog("Activating Diverter")
        active = true

        machine.events.post("diverter_${name}_activating")
        coilActivate()
        scheduleDeactivation()
    }

    /**
     * Event handler for deactivate event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventDeactivate() {
        if (!enabled) {
            return
        }
        deactivate()
    }

    /**
     * Deactivate this diverter.
     *
     * This method will disable the activation_coil, and (optionally) if it's
     * configured with a deactivation coil, it will pulse it.
     */
    fun deactivate() {
        debugLog("Deactivating Diverter")
        active = false

        val activationTime = config["activation_time"] as? Long
        if (activationTime != null) {
            delay.remove("deactivate_timed")
        }

        machine.events.post("diverter_${name}_deactivating")
        coilDeactivate()
    }

    /**
     * Schedule a delay to deactivate this diverter.
     */
    private fun scheduleDeactivation() {
        val activationTime = config["activation_time"] as? Long
        if (activationTime != null) {
            delay.add(activationTime, "deactivate_timed") {
                deactivate()
            }
        }
    }

    /**
     * Register switch handler on activation switches.
     */
    private fun enableSwitches() {
        val activationSwitches = config["activation_switches"] as? List<*> ?: emptyList<Any>()
        debugLog("Enabling Diverter sw switches: $activationSwitches")

        for (switch in activationSwitches) {
            // TODO: Register switch handler when available
            /*
            machine.switchController.addSwitchHandlerObj(switch, ::activate)
            */
        }
    }

    /**
     * Deregister switch handlers for activation switches.
     */
    private fun disableSwitches() {
        val activationSwitches = config["activation_switches"] as? List<*> ?: emptyList<Any>()
        debugLog("Disabling Diverter sw switches: $activationSwitches")

        for (switch in activationSwitches) {
            // TODO: Remove switch handler when available
            /*
            machine.switchController.removeSwitchHandler(
                switchName = switch.name,
                callback = ::activate
            )
            */
        }
    }

    /**
     * Feeder eject count decrease handler.
     *
     * @param target Target ball device
     */
    private fun feederEjectCountDecrease(target: Any) {
        debugLog("Source reported success")

        val coolDownTime = (config["cool_down_time"] as? Number)?.toLong()
        if (coolDownTime != null) {
            delay.add(coolDownTime, "reduce_eject_count") {
                reduceEjectCount()
            }
        } else {
            reduceEjectCount()
        }
    }

    /**
     * Reduce the eject count.
     */
    private fun reduceEjectCount() {
        divertingEjectsCount--
        if (divertingEjectsCount <= 0) {
            divertingEjectsCount = 0

            // If there are ejects waiting for the other target switch diverter
            if (ejectAttemptQueue.isNotEmpty()) {
                val allowMultiple = config["allow_multiple_concurrent_ejects_to_same_side"] as? Boolean ?: false

                // And perform those ejects
                if (allowMultiple) {
                    while (ejectAttemptQueue.isNotEmpty()) {
                        divertingEjectsCount++
                        val queue = ejectAttemptQueue.removeFirst()
                        // TODO: Clear queue when available
                        // queue.clear()
                    }
                } else {
                    if (ejectAttemptQueue.isNotEmpty()) {
                        divertingEjectsCount++
                        val queue = ejectAttemptQueue.removeFirst()
                        // TODO: Clear queue when available
                        // queue.clear()
                    }
                }
            } else if (active && config["activation_time"] == null) {
                // If diverter is active and no more ejects are ongoing
                deactivate()
            }
        }
    }

    /**
     * Get desired state for a target.
     *
     * @param target Target ball device
     * @return True for active targets, false for inactive targets, null if unknown
     */
    private fun getDesiredState(target: Any): Boolean? {
        val targetsWhenActive = config["targets_when_active"] as? List<*> ?: emptyList<Any>()
        val targetsWhenInactive = config["targets_when_inactive"] as? List<*> ?: emptyList<Any>()

        return when {
            target in targetsWhenActive -> true
            target in targetsWhenInactive -> false
            else -> null
        }
    }

    /**
     * Feeder eject attempt handler.
     *
     * @param queue Eject queue
     * @param target Target ball device
     */
    private fun feederEjectAttempt(queue: Any, target: Any) {
        debugLog("Feeder device eject attempt for target: $target")

        val desiredState = getDesiredState(target)

        if (desiredState == null) {
            debugLog("Feeder device ejects to an unknown target: $target. Ignoring!")
            return
        }

        if (divertingEjectsCount > 0) {
            val allowMultiple = config["allow_multiple_concurrent_ejects_to_same_side"] as? Boolean ?: false

            if (allowMultiple && ejectState != desiredState) {
                debugLog(
                    "Feeder devices tries to eject to a target which would require a state change. " +
                    "Postponing that because we have an eject to the other side"
                )
                // TODO: Queue wait when available
                // queue.wait()
                ejectAttemptQueue.add(queue)
                return
            }

            if (!allowMultiple) {
                debugLog("More than one eject and allow_multiple_concurrent_ejects_to_same_side is false")
                // TODO: Queue wait when available
                // queue.wait()
                ejectAttemptQueue.add(queue)
                return
            }
        }

        divertingEjectsCount++
        ejectState = desiredState
    }

    /**
     * Enable or disable diverter on eject.
     *
     * @param target Target ball device
     */
    private fun feederEjecting(target: Any) {
        debugLog("Feeder device is ejecting for target: $target")

        val desiredState = getDesiredState(target)

        if (desiredState == null) {
            debugLog("Feeder device ejects to an unknown target: $target. Ignoring!")
            return
        }

        if (desiredState) {
            debugLog("Enabling diverter since eject target is on the active target list")
            enable()
        } else {
            debugLog("Disabling diverter since eject target is on the inactive target list")
            disable()
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
        val ballSearchHoldTime = (config["ball_search_hold_time"] as? Number)?.toLong() ?: 1000L

        if (active) {
            debugLog("Temporarily deactivating diverter to search ball for ${ballSearchHoldTime}ms")
            coilDeactivate()

            delay.add(ballSearchHoldTime, "diverter_${name}_ball_search") {
                coilActivate()
            }
        } else {
            debugLog("Temporarily activating diverter to search ball for ${ballSearchHoldTime}ms")
            coilActivate()

            delay.add(ballSearchHoldTime, "diverter_${name}_ball_search") {
                coilDeactivate()
            }
        }

        return true
    }

    /**
     * Restore state after ball search ended.
     */
    private fun ballSearchRestore() {
        if (active) {
            coilActivate()
        } else {
            coilDeactivate()
        }
    }
}

/**
 * Event: diverter_(name)_enabling
 *
 * The diverter called (name) is enabling itself. Note that if this diverter has
 * activation_switches configured, it will not physically activate until one of
 * those switches is hit. Otherwise this diverter will activate immediately.
 */

/**
 * Event: diverter_(name)_disabling
 *
 * The diverter called (name) is disabling itself. Note that if this diverter has
 * activation_switches configured, it will not physically deactivate now, instead
 * deactivating based on switch hits and timing. Otherwise this diverter will
 * deactivate immediately.
 */

/**
 * Event: diverter_(name)_activating
 *
 * The diverter called (name) is activating itself, which means it's physically
 * pulsing or holding the coil to move.
 */

/**
 * Event: diverter_(name)_deactivating
 *
 * The diverter called (name) is deactivating itself.
 */
