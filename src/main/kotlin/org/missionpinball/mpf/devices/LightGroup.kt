package org.missionpinball.mpf.devices

import kotlinx.coroutines.CompletableDeferred
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.RGBColor
import org.missionpinball.mpf.core.SystemWideDevice
import kotlin.math.*

/**
 * An abstract group of lights.
 *
 * Light groups create multiple light devices based on a template configuration.
 * They are useful for creating LED strips, rings, and other multi-light displays
 * without having to manually configure each individual light.
 */
abstract class LightGroup(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    /**
     * List of lights in this group.
     */
    val lights = mutableListOf<Light>()

    /**
     * Future that completes when all drivers are loaded.
     */
    private val driversLoaded = CompletableDeferred<Boolean>()

    companion object {
        /**
         * Prepare config by adding light_template if not present.
         *
         * @param config Unparsed config
         * @param isModeConfig Whether this is in mode (not used)
         * @return Prepared config
         */
        fun prepareConfig(config: MutableMap<String, Any?>, isModeConfig: Boolean): MutableMap<String, Any?> {
            if ("light_template" !in config) {
                config["light_template"] = mutableMapOf<String, Any?>()
            }
            return config
        }
    }

    override suspend fun initialize() {
        super.initialize()

        val reorder = createLights()

        for (light in lights) {
            light.deviceAddedSystemWide()
        }

        if (reorder) {
            reorderLights()
        }

        driversLoaded.complete(true)
    }

    /**
     * Return all lights in group as token.
     *
     * @return Token map with lights
     */
    fun getToken(): Map<String, Any> {
        return mapOf("lights" to lights)
    }

    /**
     * Create a light at a specific index.
     *
     * @param index Absolute index in the numbering sequence
     * @param x X coordinate (or null)
     * @param y Y coordinate (or null)
     * @param relativeIndex Index relative to this group (0-based)
     */
    protected fun createLightAtIndex(index: Int, x: Double?, y: Double?, relativeIndex: Int) {
        val light = Light(machine, "${name}_light_$relativeIndex")

        val tags = mutableListOf(name)
        val configTags = config["tags"] as? List<*> ?: emptyList<String>()
        tags.addAll(configTags.map { it.toString() })

        val lightTemplate = config["light_template"] as? Map<*, *> ?: emptyMap<String, Any?>()
        val lightConfig = lightTemplate.toMutableMap() as MutableMap<String, Any?>

        // Determine how to configure the light number/channel
        when {
            config["start_channel"] != null || config["previous"] != null -> {
                if (relativeIndex == 0) {
                    if (config["start_channel"] != null) {
                        lightConfig["start_channel"] = config["start_channel"]
                    } else {
                        // TODO: Get previous light name when available
                        // lightConfig["previous"] = config["previous"]?.name
                    }
                } else {
                    lightConfig["previous"] = "${name}_light_${relativeIndex - 1}"
                }
            }
            config["number_template"] != null -> {
                val template = config["number_template"] as String
                lightConfig["number"] = template.format(index)
            }
            else -> {
                lightConfig["number"] = index
            }
        }

        // Add this group's name as a tag
        val lightTags = (lightConfig["tags"] as? MutableList<String>) ?: mutableListOf()
        lightTags.add(name)
        lightConfig["tags"] = lightTags

        lightConfig["x"] = x
        lightConfig["y"] = y

        val validatedConfig = light.validateAndParseConfig(lightConfig, false)
        light.loadConfig(validatedConfig)

        lights.add(light)

        // TODO: Add to machine lights collection when available
        // machine.lights[light.name] = light
    }

    /**
     * Create lights for this group.
     *
     * @return True if lights should be reordered after creation
     */
    protected abstract fun createLights(): Boolean

    /**
     * Call color on all lights in this group.
     *
     * @param color Color to set
     * @param fadeMs Fade time in milliseconds
     * @param priority Priority level
     * @param key Unique key for this color setting
     */
    fun color(color: RGBColor, fadeMs: Long? = null, priority: Int = 0, key: String? = null) {
        for (light in lights) {
            light.color(color, fadeMs, priority, key)
        }
    }

    /**
     * Reorder lights based on special display configurations.
     */
    private fun reorderLights() {
        val size = config["size"] as? String

        val order = when (size) {
            "8digit" -> {
                // Magic order for 8 digit NeoSeg displays from CobraPin
                listOf(
                    95, 90, 93, 82, 85, 89, 86, 91, 88, 87, 81, 92, 83, 84, 94,
                    104, 76, 79, 96, 99, 103, 100, 77, 102, 101, 75, 78, 97, 98, 80,
                    110, 105, 108, 67, 70, 74, 71, 106, 73, 72, 66, 107, 68, 69, 109,
                    119, 61, 64, 111, 114, 118, 115, 62, 117, 116, 60, 63, 112, 113, 65,
                    5, 0, 3, 52, 55, 59, 56, 1, 58, 57, 51, 2, 53, 54, 4,
                    14, 46, 49, 6, 9, 13, 10, 47, 12, 11, 45, 48, 7, 8, 50,
                    20, 15, 18, 37, 40, 44, 41, 16, 43, 42, 36, 17, 38, 39, 19,
                    29, 31, 34, 21, 24, 28, 25, 32, 27, 26, 30, 33, 22, 23, 35
                )
            }
            "2digit" -> {
                // Magic order for 2 digit NeoSeg displays from CobraPin
                listOf(
                    5, 0, 3, 22, 25, 29, 26, 1, 28, 27, 21, 2, 23, 24, 4,
                    14, 16, 19, 6, 9, 13, 10, 17, 12, 11, 15, 18, 7, 8, 20
                )
            }
            else -> {
                (0 until lights.size).toList()
            }
        }

        val reorderedLights = order.map { lights[it] }
        lights.clear()
        lights.addAll(reorderedLights)
    }

    /**
     * Wait for all drivers to be loaded.
     *
     * @return Future that completes when loaded
     */
    suspend fun waitForLoaded() {
        driversLoaded.await()
    }
}

/**
 * A light stripe (linear arrangement of lights).
 *
 * Creates lights in a line with configurable direction and spacing.
 */
class LightStrip(machine: MachineController, name: String) : LightGroup(machine, name) {

    override val configSection = "light_stripes"
    override val collection = "light_stripes"
    override val classLabel = "light_stripe"

    override fun createLights(): Boolean {
        val numberStart = config["number_start"] as? Int ?: 0
        val count = config["count"] as? Int ?: 0
        val startX = config["start_x"] as? Double
        val startY = config["start_y"] as? Double
        val direction = config["direction"] as? Double ?: 0.0
        val distanceStep = config["distance"] as? Double ?: 1.0

        var distance = 0.0

        for (index in numberStart until numberStart + count) {
            val (x, y) = if (startX != null && startY != null) {
                val xPos = startX + sin(direction / 180.0 * PI) * distance
                val yPos = startY + cos(direction / 180.0 * PI) * distance
                distance += distanceStep
                Pair(xPos, yPos)
            } else {
                Pair(null, null)
            }

            val relativeIndex = index - numberStart
            createLightAtIndex(index, x, y, relativeIndex)
        }

        return false
    }
}

/**
 * A light ring (circular arrangement of lights).
 *
 * Creates lights in a circle with configurable radius and starting angle.
 */
class LightRing(machine: MachineController, name: String) : LightGroup(machine, name) {

    override val configSection = "light_rings"
    override val collection = "light_rings"
    override val classLabel = "light_ring"

    override fun createLights(): Boolean {
        val numberStart = config["number_start"] as? Int ?: 0
        val count = config["count"] as? Int ?: 0
        val centerX = config["center_x"] as? Double
        val centerY = config["center_y"] as? Double
        val radius = config["radius"] as? Double ?: 1.0
        val startAngle = config["start_angle"] as? Double ?: 0.0

        var angle = startAngle / 180.0 * PI

        for (index in numberStart until numberStart + count) {
            val (x, y) = if (centerX != null && centerY != null) {
                val xPos = centerX + sin(angle) * radius
                val yPos = centerY + cos(angle) * radius
                angle += 2 * PI / count
                Pair(xPos, yPos)
            } else {
                Pair(null, null)
            }

            val relativeIndex = index - numberStart
            createLightAtIndex(index, x, y, relativeIndex)
        }

        return false
    }
}

/**
 * A NeoSeg Display from CobraPin.
 *
 * Creates lights for a NeoSeg 7-segment display with special ordering.
 * Supports 2-digit and 8-digit displays.
 */
class NeoSegDisplay(machine: MachineController, name: String) : LightGroup(machine, name) {

    override val configSection = "neoseg_displays"
    override val collection = "neoseg_displays"
    override val classLabel = "neoseg_display"

    override fun createLights(): Boolean {
        val numberStart = config["number_start"] as? Int ?: 0
        val size = config["size"] as? String

        val count = when (size) {
            "8digit" -> 120
            "2digit" -> 30
            else -> 0
        }

        for (index in numberStart until numberStart + count) {
            val relativeIndex = index - numberStart
            createLightAtIndex(index, null, null, relativeIndex)
        }

        return true // Needs reordering
    }
}
