package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import kotlin.math.*

/**
 * Implements a multi-axis accelerometer.
 *
 * In modern machines, accelerometers can be used for tilt detection and to
 * detect whether a machine is properly leveled.
 *
 * The accelerometer device produces a data stream of readings which MPF
 * converts to g-forces, and then events can be posted when the "hit" (or
 * g-force) of an accelerometer exceeds a predefined threshold.
 *
 * Note: In Python, this uses @DeviceMonitor("value") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Accelerometer(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "accelerometers"
    override val collection = "accelerometers"
    override val classLabel = "accelerometer"

    /**
     * Platform for accelerometer.
     */
    var platform: Any? = null

    /**
     * Hardware accelerometer interface.
     */
    var hwDevice: Any? = null

    /**
     * Current acceleration value (x, y, z) in g-forces.
     */
    var value: Triple<Double, Double, Double>? = null
        private set

    /**
     * Smoothed historical value for comparison.
     */
    private var history: Triple<Double, Double, Double>? = null

    override suspend fun initialize() {
        super.initialize()

        // TODO: Configure platform when platform system is complete
        /*
        platform = machine.getPlatformSections("accelerometers", config["platform"])
        (platform as AccelerometerPlatform).assertHasFeature("accelerometers")

        if (platform.features["allow_empty_numbers"] != true && config["number"] == null) {
            raiseConfigError("Accelerometer must have a number.", 1)
        }

        hwDevice = platform.configureAccelerometer(
            config["number"] as String,
            config["platform_settings"] as? Map<String, Any?>,
            this
        )
        */

        logger.warn { "Accelerometer platform not yet fully implemented for $name" }
    }

    /**
     * Calculate acceleration based on readings from hardware.
     *
     * This method is called by the platform interface when new acceleration
     * data is available from the hardware.
     *
     * @param x X-axis acceleration in g-forces
     * @param y Y-axis acceleration in g-forces
     * @param z Z-axis acceleration in g-forces
     */
    fun updateAcceleration(x: Double, y: Double, z: Double) {
        value = Triple(x, y, z)

        val (dx, dy, dz) = if (history == null) {
            history = Triple(x, y, z)
            Triple(0.0, 0.0, 0.0)
        } else {
            val alpha = config["alpha"] as? Double ?: 0.5
            val oldHistory = history!!

            val deltaX = x - oldHistory.first
            val deltaY = y - oldHistory.second
            val deltaZ = z - oldHistory.third

            // Exponential smoothing filter
            history = Triple(
                oldHistory.first * alpha + x * (1 - alpha),
                oldHistory.second * alpha + y * (1 - alpha),
                oldHistory.third * alpha + z * (1 - alpha)
            )

            Triple(deltaX, deltaY, deltaZ)
        }

        handleHits(dx, dy, dz)

        // Only check level when we are in a steady state
        if (abs(dx) + abs(dy) + abs(dz) < 0.05) {
            handleLevel()
        }
    }

    /**
     * Return current 3D level (angle deviation from configured level).
     *
     * @return Angle in radians
     */
    fun getLevelXyz(): Double {
        val currentValue = value ?: return 0.0
        return calculateAngle(
            config["level_x"] as? Double ?: 0.0,
            config["level_y"] as? Double ?: 0.0,
            config["level_z"] as? Double ?: 1.0,
            currentValue.first,
            currentValue.second,
            currentValue.third
        )
    }

    /**
     * Return current 2D x/z level.
     *
     * @return Angle in radians
     */
    fun getLevelXz(): Double {
        val currentValue = value ?: return 0.0
        return calculateAngle(
            config["level_x"] as? Double ?: 0.0,
            0.0,
            config["level_z"] as? Double ?: 1.0,
            currentValue.first,
            0.0,
            currentValue.third
        )
    }

    /**
     * Return current 2D y/z level.
     *
     * @return Angle in radians
     */
    fun getLevelYz(): Double {
        val currentValue = value ?: return 0.0
        return calculateAngle(
            0.0,
            config["level_y"] as? Double ?: 0.0,
            config["level_z"] as? Double ?: 1.0,
            0.0,
            currentValue.second,
            currentValue.third
        )
    }

    /**
     * Handle level checking and post events if thresholds are exceeded.
     */
    private fun handleLevel() {
        val deviationXyz = getLevelXyz()
        val deviationXz = getLevelXz()
        val deviationYz = getLevelYz()

        val levelLimits = config["level_limits"] as? Map<*, *> ?: emptyMap<Any, Any>()

        for ((maxDeviationKey, eventName) in levelLimits) {
            val maxDeviation = (maxDeviationKey as? Number)?.toDouble() ?: continue
            val deviationDegrees = deviationXyz / PI * 180

            if (deviationDegrees > maxDeviation) {
                debugLog(
                    "Deviation x: ${deviationXz / PI * 180}, " +
                    "y: ${deviationYz / PI * 180}, " +
                    "total: $deviationDegrees"
                )

                machine.events.post(
                    eventName as String,
                    mapOf(
                        "deviation_xyz" to deviationXyz,
                        "deviation_xz" to deviationXz,
                        "deviation_yz" to deviationYz
                    )
                )
            }
        }
    }

    /**
     * Handle hit detection and post events if thresholds are exceeded.
     *
     * @param dx Change in x acceleration
     * @param dy Change in y acceleration
     * @param dz Change in z acceleration
     */
    private fun handleHits(dx: Double, dy: Double, dz: Double) {
        val acceleration = calculateVectorLength(dx, dy, dz)
        val hitLimits = config["hit_limits"] as? Map<*, *> ?: emptyMap<Any, Any>()

        for ((minAccelerationKey, eventName) in hitLimits) {
            val minAcceleration = (minAccelerationKey as? Number)?.toDouble() ?: continue

            if (acceleration > minAcceleration) {
                debugLog(
                    "Received hit of $acceleration > $minAcceleration. " +
                    "Posting $eventName"
                )

                machine.events.post(eventName as String)
            }
        }
    }

    /**
     * Calculate the length of a 3D vector.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @return Vector length
     */
    private fun calculateVectorLength(x: Double, y: Double, z: Double): Double {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Calculate the angle between two 3D vectors.
     *
     * @param x1 First vector x
     * @param y1 First vector y
     * @param z1 First vector z
     * @param x2 Second vector x
     * @param y2 Second vector y
     * @param z2 Second vector z
     * @return Angle in radians
     */
    private fun calculateAngle(
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double
    ): Double {
        val divisor = calculateVectorLength(x1, y1, z1) * calculateVectorLength(x2, y2, z2)

        if (divisor == 0.0) {
            return 0.0
        }

        return acos((x1 * x2 + y1 * y2 + z1 * z2) / divisor)
    }
}
