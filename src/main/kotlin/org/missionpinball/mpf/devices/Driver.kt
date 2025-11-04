package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import org.missionpinball.mpf.exceptions.DriverLimitsError
import org.missionpinball.mpf.platforms.DriverConfig
import org.missionpinball.mpf.platforms.DriverPlatform
import org.missionpinball.mpf.platforms.DriverPlatformInterface
import org.missionpinball.mpf.platforms.HoldSettings
import org.missionpinball.mpf.platforms.PulseSettings

/**
 * Generic class that holds driver objects.
 *
 * A 'driver' is any device controlled from a driver board which is typically
 * the high-voltage stuff like coils and flashers.
 *
 * This class exposes the methods you should use on these driver types of
 * devices. Each platform module (i.e. P-ROC, FAST, etc.) subclasses this
 * class to actually communicate with the physical hardware and perform the
 * actions.
 */
class Driver(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "coils"
    override val collection = "coils"
    override val classLabel = "coil"

    /**
     * Hardware driver interface.
     */
    var hwDriver: DriverPlatformInterface? = null

    /**
     * Delay manager for this driver.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Platform this driver is on.
     */
    var platform: DriverPlatform? = null
        private set

    /**
     * Calculated pulse milliseconds (with placeholder support).
     */
    private var pulseMs: Int? = null

    /**
     * Calculated timed enable milliseconds (with placeholder support).
     */
    private var timedEnableMs: Int? = null

    companion object {
        /**
         * Device class initialization.
         *
         * Called once when device class is loaded.
         */
        fun deviceClassInit(machine: MachineController) {
            // TODO: Register handler for duplicate coil number checks when event system is fully integrated
            // machine.events.addHandler("init_phase_4") { checkDuplicateCoilNumbers(machine) }
        }

        /**
         * Check for duplicate coil numbers across all coils.
         */
        private fun checkDuplicateCoilNumbers(machine: MachineController) {
            val checkSet = mutableSetOf<Pair<Any?, Any>>()

            // TODO: Implement when coils collection is available
            /*
            for (coil in machine.coils.values) {
                if (coil !is Driver) {
                    // Skip dual wound and other special devices
                    continue
                }

                val key = Pair(coil.config["platform"], coil.hwDriver?.number)
                if (key in checkSet) {
                    throw IllegalStateException(
                        "Duplicate coil number ${coil.hwDriver?.number} for coil $coil"
                    )
                }
                checkSet.add(key)
            }
            */
        }
    }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): MutableMap<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // TODO: Implement when platform system is available
        /*
        val platform = machine.getPlatformSections("coils", config["platform"])
        platform.assertHasFeature("drivers")
        validatedConfig["platform_settings"] = platform.validateCoilSection(
            this,
            config["platform_settings"] as? Map<String, Any?>
        )
        */

        return validatedConfig
    }

    /**
     * Calculate pulse_ms with placeholder support.
     */
    private fun calculatePulseMs() {
        // TODO: Implement placeholder support when placeholder manager is available
        pulseMs = if (config["default_pulse_ms"] != null) {
            config["default_pulse_ms"] as Int
        } else {
            // TODO: Get from machine config when available
            10 // Default pulse milliseconds
        }
    }

    /**
     * Calculate timed_enable_ms with placeholder support.
     */
    private fun calculateTimedEnableMs() {
        // TODO: Implement placeholder support when placeholder manager is available
        timedEnableMs = if (config["default_timed_enable_ms"] != null) {
            config["default_timed_enable_ms"] as Int
        } else {
            // TODO: Get from machine config when available
            1000 // Default timed enable milliseconds
        }
    }

    override suspend fun initialize() {
        super.initialize()

        // TODO: Get platform when platform system is available
        // platform = machine.getPlatformSections("coils", config["platform"]) as DriverPlatform

        calculatePulseMs()
        calculateTimedEnableMs()

        // TODO: Configure hardware driver when platform is available
        /*
        val driverConfig = DriverConfig(
            name = name,
            defaultPulseMs = getAndVerifyPulseMs(null),
            defaultPulsePower = getAndVerifyPulsePower(null),
            defaultHoldPower = getAndVerifyHoldPower(null),
            defaultTimedEnableMs = getAndVerifyTimedEnableMs(null),
            defaultRecycle = config["default_recycle"] as? Boolean ?: false,
            maxPulseMs = config["max_pulse_ms"] as? Int ?: 255,
            maxPulsePower = config["max_pulse_power"] as? Double ?: 1.0,
            maxHoldPower = config["max_hold_power"] as? Double ?: 1.0
        )

        val platformSettings = config["platform_settings"] as? Map<String, Any?> ?: emptyMap()

        if (platform?.features?.get("allow_empty_numbers") != true && config["number"] == null) {
            throw IllegalStateException("Driver must have a number.")
        }

        try {
            hwDriver = platform?.configureDriver(driverConfig, config["number"] as String, platformSettings)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to configure driver $name in platform. See error above", e)
        }
        */
    }

    /**
     * Get and verify pulse power.
     *
     * @param pulsePower Power to verify, or null to use default
     * @return Verified pulse power (0.0 to 1.0)
     */
    fun getAndVerifyPulsePower(pulsePower: Double?): Double {
        var power = pulsePower ?: (config["default_pulse_power"] as? Double ?: 1.0)

        if (power < 0.0 || power > 1.0) {
            throw IllegalArgumentException("Pulse power has to be between 0 and 1 but is $power")
        }

        val maxPulsePower = (config["max_pulse_power"] as? Double)
            ?: (config["default_pulse_power"] as? Double)
            ?: 1.0

        if (power > maxPulsePower) {
            throw DriverLimitsError(
                "Driver $name may not be pulsed with pulse_power $power because max_pulse_power is $maxPulsePower"
            )
        }

        return power
    }

    /**
     * Get and verify hold power.
     *
     * @param holdPower Power to verify, or null to use default
     * @return Verified hold power (0.0 to 1.0)
     */
    fun getAndVerifyHoldPower(holdPower: Double?): Double {
        var power = holdPower
            ?: (config["default_hold_power"] as? Double)
            ?: (if (config["max_hold_power"] != null) config["max_hold_power"] as Double else null)
            ?: (if (config["allow_enable"] as? Boolean == true) 1.0 else 0.0)

        if (power < 0.0 || power > 1.0) {
            throw IllegalArgumentException("Hold_power has to be between 0 and 1 but is $power")
        }

        val maxHoldPower = (config["max_hold_power"] as? Double)
            ?: (if (config["allow_enable"] as? Boolean == true) 1.0 else null)
            ?: (config["default_hold_power"] as? Double)
            ?: 0.0

        if (power > maxHoldPower) {
            throw DriverLimitsError(
                "Driver $name may not be enabled with hold_power $power because max_hold_power is $maxHoldPower"
            )
        }

        return power
    }

    /**
     * Get and verify pulse milliseconds.
     *
     * @param pulseMsParam Pulse duration to verify, or null to use default
     * @return Verified pulse milliseconds
     */
    fun getAndVerifyPulseMs(pulseMsParam: Int?): Int {
        checkNotNull(platform) { "Platform must be set before verifying pulse_ms" }

        val ms = pulseMsParam ?: pulseMs ?: 10

        val maxPulse = (platform?.features?.get("max_pulse") as? Int) ?: 255
        if (ms < 0 || ms > maxPulse) {
            throw IllegalArgumentException("Pulse_ms $ms is not valid.")
        }

        val maxPulseMs = config["max_pulse_ms"] as? Int
        if (maxPulseMs != null && ms > maxPulseMs) {
            throw DriverLimitsError(
                "Driver $name may not be pulsed with pulse_ms $ms because max_pulse_ms is $maxPulseMs"
            )
        }

        return ms
    }

    /**
     * Get and verify timed enable milliseconds.
     *
     * @param timedEnableMsParam Timed enable duration to verify, or null to use default
     * @return Verified timed enable milliseconds
     */
    fun getAndVerifyTimedEnableMs(timedEnableMsParam: Int?): Int {
        checkNotNull(platform) { "Platform must be set before verifying timed_enable_ms" }

        val ms = timedEnableMsParam ?: timedEnableMs ?: 1000

        val maxHoldDuration = config["max_hold_duration"] as? Int
        if (maxHoldDuration != null && ms > maxHoldDuration) {
            throw DriverLimitsError(
                "Driver $name may not be held with timed_enable_ms $ms because max_hold_duration is $maxHoldDuration"
            )
        }

        return ms
    }

    /**
     * Event handler for enable control event.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventEnable(pulseMs: Int? = null, pulsePower: Double? = null, holdPower: Double? = null) {
        enable(pulseMs, pulsePower, holdPower)
    }

    /**
     * Enable a driver by holding it 'on'.
     *
     * @param pulseMs The number of milliseconds for the initial pulse
     * @param pulsePower The pulse power (0.0 to 1.0)
     * @param holdPower The hold power (0.0 to 1.0)
     * @param maxWaitMs Maximum time this pulse may be delayed for PSU optimization
     */
    fun enable(
        pulseMs: Int? = null,
        pulsePower: Double? = null,
        holdPower: Double? = null,
        maxWaitMs: Int? = null
    ) {
        checkNotNull(hwDriver) { "Hardware driver must be configured" }

        val verifiedPulseMs = getAndVerifyPulseMs(pulseMs)
        val waitMs = notifyPsuAndGetWaitMs(verifiedPulseMs, maxWaitMs)

        val verifiedPulsePower = getAndVerifyPulsePower(pulsePower)
        val verifiedHoldPower = getAndVerifyHoldPower(holdPower)

        if (verifiedHoldPower == 0.0) {
            throw DriverLimitsError("Cannot enable driver with hold_power 0.0")
        }

        if (waitMs > 0) {
            debugLog(
                "Delaying enable by ${waitMs}ms pulse_ms: ${verifiedPulseMs}ms " +
                "($verifiedPulsePower pulse_power $verifiedHoldPower hold_power)"
            )
            delay.add(waitMs.toLong()) {
                enableNow(verifiedPulseMs, verifiedPulsePower, verifiedHoldPower)
            }
        } else {
            enableNow(verifiedPulseMs, verifiedPulsePower, verifiedHoldPower)
        }
    }

    /**
     * Enable the driver now (internal method).
     */
    private fun enableNow(pulseMs: Int, pulsePower: Double, holdPower: Double) {
        infoLog(
            "Enabling Driver with power $holdPower (pulse_ms ${pulseMs}ms and pulse_power $pulsePower)"
        )

        hwDriver?.enable(
            PulseSettings(power = pulsePower, duration = pulseMs),
            HoldSettings(power = holdPower)
        )

        val maxHoldDuration = config["max_hold_duration"] as? Int
        if (maxHoldDuration != null) {
            delay.add((maxHoldDuration * 1000).toLong(), "enable_limit_reached") {
                enableLimitReached()
            }
        }

        // TODO: Inform BCP clients when BCP is implemented
        // machine.bcp.interface.sendDriverEvent(...)
    }

    /**
     * Handle max hold duration limit reached.
     */
    private fun enableLimitReached() {
        logger.warn { "Reached max_hold_duration for this coil. Will disable driver now to prevent damage!" }
        disable()
        // TODO: Add service alert when service module is implemented
        // machine.service.addTechnicalAlert(this, "Reached max_hold_duration. Driver disabled to prevent damage!")
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
     * Disable this driver.
     */
    fun disable() {
        infoLog("Disabling Driver")
        hwDriver?.disable()
        delay.remove("enable_limit_reached")

        // TODO: Inform BCP clients when BCP is implemented
        // machine.bcp.interface.sendDriverEvent(action="disable", name=name, number=config["number"])
    }

    /**
     * Notify PSU and get wait time for pulse.
     *
     * @param pulseMs Pulse duration
     * @param maxWaitMs Maximum wait time
     * @return Wait time in milliseconds
     */
    private fun notifyPsuAndGetWaitMs(pulseMs: Int, maxWaitMs: Int?): Int {
        // TODO: Implement PSU notification when PSU is available
        /*
        if (maxWaitMs == null) {
            config["psu"]?.notifyAboutInstantPulse(pulseMs)
            return 0
        }
        return config["psu"]?.getWaitTimeForPulse(pulseMs, maxWaitMs) ?: 0
        */
        return 0
    }

    /**
     * Pulse this driver now (internal method).
     */
    private fun pulseNow(pulseMs: Int, pulsePower: Double) {
        checkNotNull(hwDriver) { "Hardware driver must be configured" }
        checkNotNull(platform) { "Platform must be set" }

        // If this driver pulses via timed_enable, call that instead
        if (config["pulse_with_timed_enable"] as? Boolean == true) {
            timedEnable(pulseMs = pulseMs, pulsePower = pulsePower)
            return
        }

        val maxPulse = (platform?.features?.get("max_pulse") as? Int) ?: 255

        if (pulseMs in 1..maxPulse) {
            infoLog("Pulsing Driver for ${pulseMs}ms ($pulsePower pulse_power)")
            hwDriver?.pulse(PulseSettings(power = pulsePower, duration = pulseMs))
        } else {
            infoLog("Enabling Driver for ${pulseMs}ms ($pulsePower pulse_power)")
            delay.reset(pulseMs.toLong(), "timed_disable") {
                disable()
            }
            hwDriver?.enable(
                PulseSettings(power = pulsePower, duration = 0),
                HoldSettings(power = pulsePower)
            )
        }

        // TODO: Inform BCP clients when BCP is implemented
        // machine.bcp.interface.sendDriverEvent(action="pulse", name=name, number=config["number"],
        //                                       pulse_ms=pulseMs, pulse_power=pulsePower)
    }

    /**
     * Event handler for pulse control events.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventPulse(pulseMs: Int? = null, pulsePower: Double? = null, maxWaitMs: Int? = null) {
        pulse(pulseMs, pulsePower, maxWaitMs)
    }

    /**
     * Pulse this driver.
     *
     * @param pulseMs The number of milliseconds the driver should be enabled for
     * @param pulsePower The pulse power (0.0 to 1.0)
     * @param maxWaitMs Maximum time this pulse may be delayed for PSU optimization
     * @return Wait time in milliseconds
     */
    fun pulse(pulseMs: Int? = null, pulsePower: Double? = null, maxWaitMs: Int? = null): Int {
        val verifiedPulseMs = getAndVerifyPulseMs(pulseMs)
        val verifiedPulsePower = getAndVerifyPulsePower(pulsePower)
        val waitMs = notifyPsuAndGetWaitMs(verifiedPulseMs, maxWaitMs)

        if (waitMs > 0) {
            debugLog("Delaying pulse by ${waitMs}ms pulse_ms: ${verifiedPulseMs}ms ($verifiedPulsePower pulse_power)")
            delay.add(waitMs.toLong()) {
                pulseNow(verifiedPulseMs, verifiedPulsePower)
            }
        } else {
            pulseNow(verifiedPulseMs, verifiedPulsePower)
        }

        return waitMs
    }

    /**
     * Event handler for timed enable events.
     *
     * TODO: Add @EventHandler annotation when event system is fully integrated
     */
    fun eventTimedEnable(
        timedEnableMs: Int? = null,
        holdPower: Double? = null,
        pulseMs: Int? = null,
        pulsePower: Double? = null,
        maxWaitMs: Int? = null
    ) {
        timedEnable(timedEnableMs, holdPower, pulseMs, pulsePower, maxWaitMs)
    }

    /**
     * Pulse and enable this driver for an explicit amount of time.
     *
     * @param timedEnableMs The number of milliseconds the driver should be enabled for
     * @param holdPower The hold power for the enable duration (0.0 to 1.0)
     * @param pulseMs The number of milliseconds the driver should be enabled at pulse_power
     * @param pulsePower The pulse power for the initial pulse (0.0 to 1.0)
     * @param maxWaitMs Maximum time this pulse may be delayed for PSU optimization
     * @return Wait time in milliseconds
     */
    fun timedEnable(
        timedEnableMs: Int? = null,
        holdPower: Double? = null,
        pulseMs: Int? = null,
        pulsePower: Double? = null,
        maxWaitMs: Int? = null
    ): Int {
        val pulseDuration = getAndVerifyPulseMs(pulseMs)
        val verifiedPulsePower = getAndVerifyPulsePower(pulsePower)
        val holdDuration = getAndVerifyTimedEnableMs(timedEnableMs)
        val verifiedHoldPower = getAndVerifyHoldPower(holdPower)

        // Let the PSU wait for both the pulse _and_ the timed enable
        val waitMs = notifyPsuAndGetWaitMs(pulseDuration + holdDuration, maxWaitMs)

        infoLog(
            "Pulsing Driver for ${pulseDuration}ms ($verifiedPulsePower pulse_power) " +
            "with timed enable for ${holdDuration}ms ($verifiedHoldPower hold_power)"
        )

        // TODO: Detect a NotImplementedError and simulate a timed_enable
        //       with a software timer and enable+disable
        hwDriver?.timedEnable(
            PulseSettings(verifiedPulsePower, pulseDuration),
            HoldSettings(verifiedHoldPower, holdDuration)
        )

        return waitMs
    }
}
