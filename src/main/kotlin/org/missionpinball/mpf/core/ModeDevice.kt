package org.missionpinball.mpf.core

/**
 * A device in a mode.
 *
 * ModeDevice is a base class for devices that are loaded within modes
 * rather than system-wide. These devices are created when a mode starts
 * and removed when the mode stops.
 */
abstract class ModeDevice(
    machine: MachineController,
    name: String
) : Device(machine, name) {

    /**
     * The mode this device belongs to.
     */
    var mode: Mode? = null

    /**
     * Add device to a running mode.
     *
     * @param mode Mode which loaded the device
     */
    open suspend fun deviceAddedToMode(mode: Mode) {
        initialize()
    }

    /**
     * Load device in running mode.
     *
     * The mode just started.
     *
     * @param mode Mode which loaded the device
     * @param player Current active player
     */
    open fun deviceLoadedInMode(mode: Mode, player: Player) {
        this.mode = mode
    }

    /**
     * Return true if this device can exist outside of a game.
     */
    open val canExistOutsideOfGame: Boolean
        get() = false

    /**
     * Overload config in mode.
     *
     * @param mode Mode requesting the overload
     * @param config New configuration
     */
    open fun overloadConfigInMode(mode: Mode, config: MutableMap<String, Any?>) {
        throw IllegalStateException("Device $this cannot be overloaded.")
    }

    /**
     * Event handler for enable event.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable handler.
     *
     * Override in subclass to implement enable behavior.
     */
    open fun enable() {
        // Override in subclass
    }

    /**
     * Add control events in mode if this device has any mode control events.
     *
     * @param mode Mode which loaded the device
     */
    open fun addControlEventsInMode(mode: Mode) {
        // TODO: Implement when event system is fully integrated
        /*
        if (config["enable_events"] != null && config["enable_events"] == false) {
            mode.addModeEventHandler(
                "mode_${mode.name}_started",
                ::eventEnable,
                priority = 100
            )
        }
        */
    }

    /**
     * Remove control events.
     *
     * Override in subclass if needed.
     */
    open fun removeControlEventsInMode() {
        // Override in subclass
    }

    /**
     * Remove device because mode is unloading.
     *
     * Device object will continue to exist and may be added to the mode again later.
     *
     * @param mode Mode which stopped
     */
    open fun deviceRemovedFromMode(mode: Mode) {
        this.mode = null
    }
}
