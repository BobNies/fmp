package org.missionpinball.mpf.core

/**
 * RGB Color with alpha channel.
 *
 * Extends RGBColor to add opacity/alpha channel support for
 * colors with transparency.
 */
class RGBAColor : RGBColor {

    /**
     * Opacity/alpha channel (0-255).
     */
    var opacity: Int

    /**
     * Construct RGBA color from various input formats.
     *
     * @param color RGB color, hex string, or tuple/list with 3 or 4 components
     */
    constructor(color: Any) : super(color) {
        opacity = when (color) {
            is List<*> -> {
                if (color.size == 4) {
                    (color[3] as? Number)?.toInt() ?: 255
                } else {
                    255
                }
            }
            is IntArray -> {
                if (color.size == 4) {
                    color[3]
                } else {
                    255
                }
            }
            else -> 255
        }
    }

    /**
     * Construct RGBA color from RGB color and opacity.
     *
     * @param color RGB color
     * @param opacity Opacity value (0-255)
     */
    constructor(color: RGBColor, opacity: Int = 255) : super(color.rgb.toList()) {
        this.opacity = opacity
    }

    /**
     * Construct RGBA color from RGBA components.
     *
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @param opacity Opacity component (0-255)
     */
    constructor(red: Int, green: Int, blue: Int, opacity: Int = 255) : super(listOf(red, green, blue)) {
        this.opacity = opacity
    }

    /**
     * Iterator to support iteration over RGBA components.
     */
    operator fun iterator(): Iterator<Int> {
        return listOf(red, green, blue, opacity).iterator()
    }

    override fun toString(): String {
        return "$rgb Opacity: $opacity"
    }

    /**
     * Get RGBA representation as a 4-tuple.
     */
    val rgba: IntArray
        get() = intArrayOf(red, green, blue, opacity)

    /**
     * Set RGBA from a 4-tuple.
     *
     * @param value RGBA values as array [r, g, b, a]
     */
    fun setRgba(value: IntArray) {
        require(value.size == 4) { "RGBA value must have 4 components" }
        setRgb(intArrayOf(value[0], value[1], value[2]))
        opacity = value[3]
    }

    /**
     * Set RGBA from a list.
     *
     * @param value RGBA values as list [r, g, b, a]
     */
    fun setRgba(value: List<Int>) {
        require(value.size == 4) { "RGBA value must have 4 components" }
        setRgb(listOf(value[0], value[1], value[2]))
        opacity = value[3]
    }
}
