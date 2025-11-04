package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * An instance of a dual wound coil which consists of two coils.
 *
 * Dual wound coils have two separate windings - a main (power) coil for
 * initial activation and a hold coil for maintaining the state with less
 * power. This is commonly used for devices like flippers and diverters
 * that need strong initial activation but can hold with less power.
 */
class DualWoundCoil(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "dual_wound_coils"
    override val collection = "dual_wound_coils"
    override val classLabel = "dual_wound_coil"

    override suspend fun initialize() {
        super.initialize()

        // Add this device to the coils collection
        // TODO: Implement when device collections are available
        /*
        machine.coils[name] = this
        */
    }

    /**
     * Event handler for enable event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable a dual wound coil.
     *
     * Pulse main coil and enable hold coil. This provides strong initial
     * activation followed by sustained hold with less power.
     */
    fun enable() {
        val mainCoil = config["main_coil"] as? Driver
        val holdCoil = config["hold_coil"] as? Driver

        mainCoil?.pulse()
        holdCoil?.enable()
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    /**
     * Disable a dual wound coil.
     *
     * Disables both the main and hold coils.
     */
    fun disable() {
        val mainCoil = config["main_coil"] as? Driver
        val holdCoil = config["hold_coil"] as? Driver

        mainCoil?.disable()
        holdCoil?.disable()
    }

    /**
     * Event handler for pulse event.
     *
     * TODO: Add @EventHandler(3) annotation when event system is fully integrated
     *
     * @param milliseconds Pulse duration in milliseconds (optional)
     * @param power Power multiplier (optional, typically 0.0 to 1.0)
     */
    fun eventPulse(milliseconds: Int? = null, power: Double? = null) {
        pulse(milliseconds, power)
    }

    /**
     * Pulse this dual wound coil.
     *
     * Pulses both the main and hold coils for the specified duration.
     *
     * @param milliseconds The number of milliseconds the driver should be
     *                     enabled for. If no value is provided, the driver will be
     *                     enabled for the value specified in the config dictionary.
     * @param power A multiplier that will be applied to the default pulse time,
     *              typically a float between 0.0 and 1.0. (Note this can only be used
     *              if milliseconds is also specified.)
     */
    fun pulse(milliseconds: Int? = null, power: Double? = null) {
        val mainCoil = config["main_coil"] as? Driver
        val holdCoil = config["hold_coil"] as? Driver

        mainCoil?.pulse(milliseconds, power)
        holdCoil?.pulse(milliseconds, power)
    }
}
