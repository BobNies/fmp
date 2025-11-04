package org.missionpinball.mpf.platforms

import org.missionpinball.mpf.core.LogMixin
import org.missionpinball.mpf.core.MachineController

/**
 * Pulse settings for a driver.
 *
 * @property power Power level for the pulse (0.0 to 1.0)
 * @property duration Duration of the pulse in milliseconds
 */
data class PulseSettings(
    val power: Double,
    val duration: Int
)

/**
 * Hold settings for a driver.
 *
 * @property power Power level for the hold (0.0 to 1.0), null for no hold
 * @property duration Duration of the hold in milliseconds, null for indefinite
 */
data class HoldSettings(
    val power: Double? = null,
    val duration: Int? = null
)

/**
 * Driver configuration.
 *
 * Configuration for a driver (coil) on a hardware platform.
 */
data class DriverConfig(
    val name: String,
    val defaultPulseMs: Int,
    val defaultPulsePower: Double,
    val defaultHoldPower: Double,
    val defaultTimedEnableMs: Int,
    val defaultRecycle: Boolean,
    val maxPulseMs: Int,
    val maxPulsePower: Double,
    val maxHoldPower: Double
)

/**
 * Repulse settings for a driver.
 *
 * Settings for automatic repulsing on switch changes.
 */
data class RepulseSettings(
    val enableRepulse: Boolean,
    val debounceMs: Int
)

/**
 * Platform interface for a driver.
 *
 * Abstract interface that must be implemented by hardware platforms
 * to support driver (coil) operations.
 */
abstract class DriverPlatformInterface(
    val number: Any,
    val config: DriverConfig
) {
    /**
     * Pulse this driver for a specified duration.
     *
     * @param pulseSettings Pulse power and duration settings
     */
    abstract fun pulse(pulseSettings: PulseSettings)

    /**
     * Enable this driver (hold it on indefinitely).
     *
     * @param pulseSettings Initial pulse settings
     * @param holdSettings Hold settings
     */
    abstract fun enable(pulseSettings: PulseSettings, holdSettings: HoldSettings)

    /**
     * Disable this driver.
     */
    abstract fun disable()

    /**
     * Enable this driver for a pre-specified duration.
     *
     * @param pulseSettings Initial pulse settings
     * @param holdSettings Hold settings with duration
     */
    abstract fun timedEnable(pulseSettings: PulseSettings, holdSettings: HoldSettings)

    /**
     * Get the name of the board this driver is on.
     *
     * @return Board name
     */
    abstract fun getBoardName(): String

    override fun toString(): String {
        return "<Driver ${getBoardName()} $number (config: $config)>"
    }
}

/**
 * Base platform class.
 *
 * Abstract base class for all hardware platforms in MPF.
 */
abstract class BasePlatform(
    protected val machine: MachineController
) : LogMixin {

    /**
     * Platform features dictionary.
     * Each platform sets this to indicate which features it supports.
     */
    val features = mutableMapOf<String, Any>(
        "has_dmds" to false,
        "has_rgb_dmds" to false,
        "has_accelerometers" to false,
        "has_i2c" to false,
        "has_servos" to false,
        "has_lights" to false,
        "has_switches" to false,
        "has_drivers" to false,
        "tickless" to false,
        "has_segment_displays" to false,
        "has_hardware_sound_systems" to true,
        "has_steppers" to false,
        "allow_empty_numbers" to false,
        "hardware_eos_repulse" to false
    )

    /**
     * Debug mode flag.
     */
    var debug = false

    /**
     * Assert that this platform has a certain feature.
     *
     * @param featureName Name of the feature to check
     * @throws IllegalStateException if the feature is not supported
     */
    fun assertHasFeature(featureName: String) {
        if (features["has_$featureName"] != true) {
            throw IllegalStateException(
                "Platform ${this::class.simpleName} does not support $featureName. " +
                "Please make sure the platform you configured for $featureName actually supports that type of devices."
            )
        }
    }

    /**
     * Get information string about this platform.
     *
     * @return Information string
     */
    open fun getInfoString(): String = "Not implemented"

    /**
     * Initialize the platform.
     *
     * Called after all platforms have been created and core modules have been loaded.
     */
    open suspend fun initialize() {
        // Default implementation does nothing
    }

    /**
     * Start receiving switch changes from this platform.
     */
    open suspend fun start() {
        // Default implementation does nothing
    }

    /**
     * Periodic tick for platform updates.
     *
     * Called periodically for platforms that need to poll hardware.
     */
    open fun tick() {
        // Default implementation does nothing
    }

    /**
     * Stop the platform.
     *
     * Called when MPF stops. Platforms should gracefully clean up resources.
     */
    open fun stop() {
        // Default implementation does nothing
    }

    companion object {
        /**
         * Get config spec for this platform.
         */
        fun getConfigSpec(): Boolean = false
    }
}

/**
 * Platform that supports drivers (coils).
 */
abstract class DriverPlatform(machine: MachineController) : BasePlatform(machine) {

    init {
        // Set driver features
        features["has_drivers"] = true
        features["max_pulse"] = 255
    }

    /**
     * Configure a driver on this platform.
     *
     * @param config Driver configuration
     * @param number Platform-specific driver number
     * @param platformSettings Platform-specific settings
     * @return Driver platform interface
     */
    abstract fun configureDriver(
        config: DriverConfig,
        number: String,
        platformSettings: Map<String, Any?>?
    ): DriverPlatformInterface

    /**
     * Validate coil section of config.
     *
     * @param device Device being configured
     * @param platformSettings Platform-specific settings to validate
     * @return Validated platform settings
     */
    open fun validateCoilSection(
        device: Any,
        platformSettings: Map<String, Any?>?
    ): Map<String, Any?> {
        // Default implementation returns settings unchanged
        return platformSettings ?: emptyMap()
    }
}
