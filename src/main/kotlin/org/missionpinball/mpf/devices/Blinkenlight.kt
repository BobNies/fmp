package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.RGBColor
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A light that alternates between several different colors.
 *
 * The blinkenlight manages a list of colors with associated keys and priorities,
 * cycling through them at a configured rate. Colors are sorted by priority,
 * with higher priority colors displayed first in the cycle.
 *
 * Note: In Python, this uses @DeviceMonitor("num_colors") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Blinkenlight(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "blinkenlights"
    override val collection = "blinkenlights"
    override val classLabel = "blinkenlight"

    /**
     * List of colors as tuples of (color, key, priority).
     */
    private val colors = mutableListOf<Triple<RGBColor, String, Int>>()

    /**
     * Delay manager for scheduling color changes.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Duration per color in milliseconds (when using color_duration mode).
     */
    private var colorDuration: Long? = null

    /**
     * Total cycle duration in milliseconds (when using cycle_duration mode).
     */
    private var cycleDuration: Long? = null

    /**
     * Number of colors currently in the cycle.
     */
    var numColors: Int = 0
        private set

    /**
     * Cached light key for performance (string operations are expensive).
     */
    private val lightKey = "blinkenlight_$name"

    /**
     * The light this blinkenlight controls.
     */
    val light: Any?
        get() = config["light"]

    /**
     * Number of colors in cycle, including the "off" color if present.
     */
    val numColorsInCycle: Int
        get() = numColors + (if (offBetweenCycles()) 1 else 0)

    override fun loadConfig(config: MutableMap<String, Any?>) {
        super.loadConfig(config)

        colorDuration = (config["color_duration"] as? Number)?.toLong()
        cycleDuration = (config["cycle_duration"] as? Number)?.toLong()

        if ((colorDuration == null && cycleDuration == null) ||
            (colorDuration != null && cycleDuration != null)
        ) {
            raiseConfigError("Either color_duration or cycle_duration must be specified, but not both.", 1)
        }
    }

    /**
     * Add a color to the blinkenlight.
     *
     * If a color with the given key already exists, it will be replaced.
     *
     * @param color RGB color to add
     * @param key Unique key for this color
     * @param priority Priority for this color (higher = displayed earlier in cycle)
     */
    fun addColor(color: RGBColor, key: String, priority: Int) {
        // Remove any existing color with this key
        colors.removeAll { it.second == key }

        colors.add(Triple(color, key, priority))
        infoLog("Color $color with key $key added")
        updateLight()
    }

    /**
     * Remove all colors from the blinkenlight.
     */
    fun removeAllColors() {
        colors.clear()
        infoLog("All colors removed")
        updateLight()
    }

    /**
     * Remove a color with a given key from the blinkenlight.
     *
     * @param key Key of the color to remove
     */
    fun removeColorWithKey(key: String) {
        val oldSize = colors.size
        colors.removeAll { it.second == key }

        if (colors.size != oldSize) {
            infoLog("Color removed with key $key")
            updateLight()
        }
    }

    /**
     * Update the underlying light.
     */
    private fun updateLight() {
        delay.clear()
        numColors = colors.size

        if (colors.isNotEmpty()) {
            sortColors()
        }

        performStep()
    }

    /**
     * Sort the blinkenlight's colors by their priority (highest first).
     */
    private fun sortColors() {
        colors.sortByDescending { it.third } // third element is priority
    }

    /**
     * Determine if light should be off between cycles.
     *
     * @return True if light should turn off between cycles
     */
    private fun offBetweenCycles(): Boolean {
        val offWhenMultiple = config["off_when_multiple"] as? Boolean ?: false
        return (offWhenMultiple && colors.size > 1) || (colors.size == 1)
    }

    /**
     * Get the time between color changes in milliseconds.
     *
     * @return Time in milliseconds
     */
    private fun getTimeBetweenColorsMs(): Long {
        val delay = if (cycleDuration != null) {
            cycleDuration!! / numColorsInCycle
        } else {
            colorDuration!!
        }

        return delay.coerceAtLeast(1L)
    }

    /**
     * Get the total cycle time in milliseconds.
     *
     * @return Total cycle time in milliseconds
     */
    private fun getTotalCycleMs(): Long {
        return if (cycleDuration != null) {
            cycleDuration!!
        } else {
            colorDuration!! * numColorsInCycle
        }
    }

    /**
     * Get the current color based on the current time.
     *
     * @return Current color in the cycle
     */
    private fun getCurrentColor(): RGBColor {
        val now = machine.clock.getTime()
        val offsetMs = (now * 1000) % getTotalCycleMs()
        val colorIndex = (offsetMs / getTimeBetweenColorsMs() + 0.5).toInt()

        if (colorIndex >= numColors) {
            return RGBColor("off")
        }

        return colors[colorIndex].first
    }

    /**
     * Perform a single step in the blinkenlight cycle.
     *
     * This sets the light to the current color and schedules the next step.
     */
    private fun performStep() {
        if (numColors == 0) {
            // TODO: Remove from light stack when light system is available
            /*
            light?.removeFromStackByKey(lightKey)
            */
            return
        }

        val currentColor = getCurrentColor()
        val colorDurationMs = getTimeBetweenColorsMs()
        val cycleMs = getTotalCycleMs()
        val priority = config["priority"] as? Int ?: 0

        // TODO: Set light color when light system is available
        /*
        light?.color(currentColor, priority = priority, key = lightKey)
        */

        // Calculate delay to next color change
        val delayMs = colorDurationMs -
                ((machine.clock.getTime() * 1000) % cycleMs) % colorDurationMs

        delay.add(delayMs, "perform_step") {
            performStep()
        }
    }
}
