package org.missionpinball.mpf.core

/**
 * A system wide device which can be defined in the main config.
 *
 * This is a base class for devices that are defined in the machine-wide
 * configuration (as opposed to mode-specific devices).
 */
abstract class SystemWideDevice(
    machine: MachineController,
    name: String
) : Device(machine, name) {

    /**
     * Add the device system wide.
     *
     * Called when the device is added to the system-wide configuration.
     * This triggers device initialization.
     */
    open suspend fun deviceAddedSystemWide() {
        initialize()
    }
}
