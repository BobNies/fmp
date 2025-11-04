package org.missionpinball.mpf.devices

/**
 * Adds x/y/z position getters to a device.
 *
 * For devices that have x/y/z config in the YAML, this mixin provides
 * convenient property access instead of using config map lookups.
 *
 * Usage: Have your device implement this interface and it will automatically
 * get x, y, and z properties that read from the config map.
 */
interface DevicePositionMixin {

    /**
     * Device configuration map.
     * Must be provided by the implementing class.
     */
    val config: Map<String, Any?>

    /**
     * Get the X position value from the config.
     *
     * @return Device's x position from config, or null if not set
     */
    val x: Any?
        get() = config["x"]

    /**
     * Get the Y position value from the config.
     *
     * @return Device's y position from config, or null if not set
     */
    val y: Any?
        get() = config["y"]

    /**
     * Get the Z position value from the config.
     *
     * @return Device's z position from config, or null if not set
     */
    val z: Any?
        get() = config["z"]
}
