package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import org.missionpinball.mpf.platforms.DriverConfig
import org.missionpinball.mpf.platforms.DriverPlatformInterface
import org.missionpinball.mpf.platforms.HoldSettings
import org.missionpinball.mpf.platforms.PulseSettings

/**
 * A digital output on either a light or driver platform.
 *
 * Digital outputs are simple on/off devices that can be controlled
 * by either a driver (coil) platform or a light platform, depending
 * on the hardware configuration.
 */
class DigitalOutput(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "digital_outputs"
    override val collection = "digital_outputs"
    override val classLabel = "digital_output"

    /**
     * Hardware driver interface (either driver or light).
     */
    var hwDriver: Any? = null

    /**
     * Platform this device is on.
     */
    var platform: Any? = null

    /**
     * Type of output ("driver" or "light").
     */
    var type: String? = null

    /**
     * Delay manager for this output.
     */
    private val delay = DelayManager(machine.clock)

    override suspend fun initialize() {
        super.initialize()

        when (config["type"]) {
            "driver" -> initializeDriver()
            "light" -> initializeLight()
            else -> throw IllegalArgumentException("Invalid type ${config["type"]}")
        }
    }

    /**
     * Configure a light as digital output.
     */
    private fun initializeLight() {
        // TODO: Implement when light platform is available
        /*
        platform = machine.getPlatformSections("lights", config["platform"])
        (platform as BasePlatform).assertHasFeature("lights")
        type = "light"

        if (platform.features["allow_empty_numbers"] != true && config["number"] == null) {
            raiseConfigError("Digital Output must have a number.", 1)
        }

        val lightConfig = LightConfig(
            name = name,
            color = LightConfigColors.NONE
        )

        try {
            hwDriver = platform.configureLight(
                config["number"] as String,
                config["light_subtype"] as String,
                lightConfig,
                emptyMap()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to configure light $name in platform. See error above", e)
        }
        */
        type = "light"
        logger.warn { "Light platform not yet implemented for digital output $name" }
    }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): MutableMap<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // TODO: Implement when platform system is complete
        /*
        when (validatedConfig["type"]) {
            "driver" -> {
                val platform = machine.getPlatformSections("coils", validatedConfig["platform"])
                platform.assertHasFeature("drivers")
                validatedConfig["platform_settings"] = platform.validateCoilSection(
                    this,
                    validatedConfig["platform_settings"] as? Map<String, Any?>
                )
            }
            "light" -> {
                val platform = machine.getPlatformSections("lights", validatedConfig["platform"])
                platform.assertHasFeature("lights")
            }
            else -> throw IllegalArgumentException("Invalid type ${validatedConfig["type"]}")
        }
        */

        return validatedConfig
    }

    /**
     * Configure a driver as digital output.
     */
    private fun initializeDriver() {
        // TODO: Implement when platform system is complete
        /*
        platform = machine.getPlatformSections("coils", config["platform"])
        (platform as DriverPlatform).assertHasFeature("drivers")
        type = "driver"

        val driverConfig = DriverConfig(
            name = name,
            defaultPulseMs = 255,
            defaultPulsePower = 1.0,
            defaultTimedEnableMs = 0,
            defaultHoldPower = 1.0,
            defaultRecycle = false,
            maxPulseMs = 255,
            maxPulsePower = 1.0,
            maxHoldPower = 1.0
        )

        if (platform.features["allow_empty_numbers"] != true && config["number"] == null) {
            raiseConfigError("Digital Output must have a number.", 2)
        }

        try {
            hwDriver = platform.configureDriver(
                driverConfig,
                config["number"] as String,
                config["platform_settings"] as? Map<String, Any?>
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to configure driver $name in platform. See error above", e)
        }
        */
        type = "driver"
        logger.warn { "Driver platform not yet fully implemented for digital output $name" }
    }

    /**
     * Event handler for pulse control event.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventPulse(pulseMs: Int) {
        pulse(pulseMs)
    }

    /**
     * Pulse digital output.
     *
     * @param pulseMs Pulse duration in milliseconds
     */
    fun pulse(pulseMs: Int) {
        when (type) {
            "driver" -> {
                val driver = hwDriver as? DriverPlatformInterface
                driver?.pulse(PulseSettings(power = 1.0, duration = pulseMs))
            }
            "light" -> {
                // TODO: Implement light pulse when light platform is available
                /*
                hwDriver.setFade(1.0, -1, 1.0, -1)
                platform.lightSync()
                */
                delay.reset(pulseMs.toLong(), "timed_disable") {
                    disable()
                }
            }
            else -> throw IllegalArgumentException("Invalid type $type")
        }
    }

    /**
     * Event handler for enable control event.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable digital output.
     */
    fun enable() {
        when (type) {
            "driver" -> {
                val driver = hwDriver as? DriverPlatformInterface
                driver?.enable(
                    PulseSettings(power = 1.0, duration = 0),
                    HoldSettings(power = 1.0, duration = null)
                )
            }
            "light" -> {
                // TODO: Implement light enable when light platform is available
                /*
                hwDriver.setFade(1.0, -1, 1.0, -1)
                platform.lightSync()
                */
                delay.remove("timed_disable")
            }
            else -> throw IllegalArgumentException("Invalid type $type")
        }
    }

    /**
     * Event handler for disable control event.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    /**
     * Disable digital output.
     */
    fun disable() {
        when (type) {
            "driver" -> {
                val driver = hwDriver as? DriverPlatformInterface
                driver?.disable()
            }
            "light" -> {
                // TODO: Implement light disable when light platform is available
                /*
                hwDriver.setFade(0.0, -1, 0.0, -1)
                platform.lightSync()
                */
                delay.remove("timed_disable")
            }
            else -> throw IllegalArgumentException("Invalid type $type")
        }
    }
}
