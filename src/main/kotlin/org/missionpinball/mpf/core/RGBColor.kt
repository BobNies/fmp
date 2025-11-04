package org.missionpinball.mpf.core

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * RGB color representation with utilities.
 * Based on the colorutils open-source library.
 */
data class RGBColor(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    init {
        require(red in 0..255) { "Red value must be 0-255, got $red" }
        require(green in 0..255) { "Green value must be 0-255, got $green" }
        require(blue in 0..255) { "Blue value must be 0-255, got $blue" }
    }

    /**
     * RGB tuple representation.
     */
    val rgb: Triple<Int, Int, Int>
        get() = Triple(red, green, blue)

    /**
     * Hex string representation.
     */
    val hex: String
        get() = String.format("%02x%02x%02x", red, green, blue)

    /**
     * Get color as integer.
     */
    fun toInt(): Int = (red shl 16) or (green shl 8) or blue

    /**
     * Blend this color with another color.
     */
    fun blend(other: RGBColor, ratio: Double = 0.5): RGBColor {
        require(ratio in 0.0..1.0) { "Ratio must be between 0 and 1" }
        val r = (red * (1 - ratio) + other.red * ratio).roundToInt()
        val g = (green * (1 - ratio) + other.green * ratio).roundToInt()
        val b = (blue * (1 - ratio) + other.blue * ratio).roundToInt()
        return RGBColor(r, g, b)
    }

    /**
     * Multiply color by a factor (for dimming/brightening).
     */
    operator fun times(factor: Double): RGBColor {
        val r = Util.clamp((red * factor).roundToInt(), 0, 255)
        val g = Util.clamp((green * factor).roundToInt(), 0, 255)
        val b = Util.clamp((blue * factor).roundToInt(), 0, 255)
        return RGBColor(r, g, b)
    }

    /**
     * Add two colors together (with clamping).
     */
    operator fun plus(other: RGBColor): RGBColor {
        val r = Util.clamp(red + other.red, 0, 255)
        val g = Util.clamp(green + other.green, 0, 255)
        val b = Util.clamp(blue + other.blue, 0, 255)
        return RGBColor(r, g, b)
    }

    override fun toString(): String = "RGBColor($red, $green, $blue)"

    companion object {
        // Standard web colors
        val OFF = RGBColor(0, 0, 0)
        val WHITE = RGBColor(255, 255, 255)
        val RED = RGBColor(255, 0, 0)
        val GREEN = RGBColor(0, 255, 0)
        val BLUE = RGBColor(0, 0, 255)
        val YELLOW = RGBColor(255, 255, 0)
        val CYAN = RGBColor(0, 255, 255)
        val MAGENTA = RGBColor(255, 0, 255)
        val ORANGE = RGBColor(255, 165, 0)
        val PURPLE = RGBColor(128, 0, 128)
        val PINK = RGBColor(255, 192, 203)

        private val namedColors = mapOf(
            "off" to RGBColor(0, 0, 0),
            "aliceblue" to RGBColor(240, 248, 255),
            "antiquewhite" to RGBColor(250, 235, 215),
            "aqua" to RGBColor(0, 255, 255),
            "aquamarine" to RGBColor(127, 255, 212),
            "azure" to RGBColor(240, 255, 255),
            "beige" to RGBColor(245, 245, 220),
            "bisque" to RGBColor(255, 228, 196),
            "black" to RGBColor(0, 0, 0),
            "blanchedalmond" to RGBColor(255, 235, 205),
            "blue" to RGBColor(0, 0, 255),
            "blueviolet" to RGBColor(138, 43, 226),
            "brown" to RGBColor(165, 42, 42),
            "burlywood" to RGBColor(222, 184, 135),
            "cadetblue" to RGBColor(95, 158, 160),
            "chartreuse" to RGBColor(127, 255, 0),
            "chocolate" to RGBColor(210, 105, 30),
            "coral" to RGBColor(255, 127, 80),
            "cornflowerblue" to RGBColor(100, 149, 237),
            "cornsilk" to RGBColor(255, 248, 220),
            "crimson" to RGBColor(220, 20, 60),
            "cyan" to RGBColor(0, 255, 255),
            "darkblue" to RGBColor(0, 0, 139),
            "darkcyan" to RGBColor(0, 139, 139),
            "darkgoldenrod" to RGBColor(184, 134, 11),
            "darkgray" to RGBColor(169, 169, 169),
            "darkgreen" to RGBColor(0, 100, 0),
            "darkkhaki" to RGBColor(189, 183, 107),
            "darkmagenta" to RGBColor(139, 0, 139),
            "darkolivegreen" to RGBColor(85, 107, 47),
            "darkorange" to RGBColor(255, 140, 0),
            "darkorchid" to RGBColor(153, 50, 204),
            "darkred" to RGBColor(139, 0, 0),
            "darksalmon" to RGBColor(233, 150, 122),
            "darkseagreen" to RGBColor(143, 188, 143),
            "darkslateblue" to RGBColor(72, 61, 139),
            "darkslategray" to RGBColor(47, 79, 79),
            "darkturquoise" to RGBColor(0, 206, 209),
            "darkviolet" to RGBColor(148, 0, 211),
            "deeppink" to RGBColor(255, 20, 147),
            "deepskyblue" to RGBColor(0, 191, 255),
            "dimgray" to RGBColor(105, 105, 105),
            "dodgerblue" to RGBColor(30, 144, 255),
            "firebrick" to RGBColor(178, 34, 34),
            "floralwhite" to RGBColor(255, 250, 240),
            "forestgreen" to RGBColor(34, 139, 34),
            "fuchsia" to RGBColor(255, 0, 255),
            "gainsboro" to RGBColor(220, 220, 220),
            "ghostwhite" to RGBColor(248, 248, 255),
            "gold" to RGBColor(255, 215, 0),
            "goldenrod" to RGBColor(218, 165, 32),
            "gray" to RGBColor(128, 128, 128),
            "green" to RGBColor(0, 128, 0),
            "greenyellow" to RGBColor(173, 255, 47),
            "honeydew" to RGBColor(240, 255, 240),
            "hotpink" to RGBColor(255, 105, 180),
            "indianred" to RGBColor(205, 92, 92),
            "indigo" to RGBColor(75, 0, 130),
            "ivory" to RGBColor(255, 255, 240),
            "khaki" to RGBColor(240, 230, 140),
            "lavender" to RGBColor(230, 230, 250),
            "lavenderblush" to RGBColor(255, 240, 245),
            "lawngreen" to RGBColor(124, 252, 0),
            "lemonchiffon" to RGBColor(255, 250, 205),
            "lightblue" to RGBColor(173, 216, 230),
            "lightcoral" to RGBColor(240, 128, 128),
            "lightcyan" to RGBColor(224, 255, 255),
            "lightgoldenrodyellow" to RGBColor(250, 250, 210),
            "lightgreen" to RGBColor(144, 238, 144),
            "lightgrey" to RGBColor(211, 211, 211),
            "lightpink" to RGBColor(255, 182, 193),
            "lightsalmon" to RGBColor(255, 160, 122),
            "lightseagreen" to RGBColor(32, 178, 170),
            "lightskyblue" to RGBColor(135, 206, 250),
            "lightslategray" to RGBColor(119, 136, 153),
            "lightsteelblue" to RGBColor(176, 196, 222),
            "lightyellow" to RGBColor(255, 255, 224),
            "lime" to RGBColor(0, 255, 0),
            "limegreen" to RGBColor(50, 205, 50),
            "linen" to RGBColor(250, 240, 230),
            "magenta" to RGBColor(255, 0, 255),
            "maroon" to RGBColor(128, 0, 0),
            "mediumaquamarine" to RGBColor(102, 205, 170),
            "mediumblue" to RGBColor(0, 0, 205),
            "mediumorchid" to RGBColor(186, 85, 211),
            "mediumpurple" to RGBColor(147, 112, 219),
            "mediumseagreen" to RGBColor(60, 179, 113),
            "mediumslateblue" to RGBColor(123, 104, 238),
            "mediumspringgreen" to RGBColor(0, 250, 154),
            "mediumturquoise" to RGBColor(72, 209, 204),
            "mediumvioletred" to RGBColor(199, 21, 133),
            "midnightblue" to RGBColor(25, 25, 112),
            "mintcream" to RGBColor(245, 255, 250),
            "mistyrose" to RGBColor(255, 228, 225),
            "moccasin" to RGBColor(255, 228, 181),
            "navajowhite" to RGBColor(255, 222, 173),
            "navy" to RGBColor(0, 0, 128),
            "oldlace" to RGBColor(253, 245, 230),
            "olive" to RGBColor(128, 128, 0),
            "olivedrab" to RGBColor(107, 142, 35),
            "orange" to RGBColor(255, 165, 0),
            "orangered" to RGBColor(255, 69, 0),
            "orchid" to RGBColor(218, 112, 214),
            "palegoldenrod" to RGBColor(238, 232, 170),
            "palegreen" to RGBColor(152, 251, 152),
            "paleturquoise" to RGBColor(175, 238, 238),
            "palevioletred" to RGBColor(219, 112, 147),
            "papayawhip" to RGBColor(255, 239, 213),
            "peachpuff" to RGBColor(255, 218, 185),
            "peru" to RGBColor(205, 133, 63),
            "pink" to RGBColor(255, 192, 203),
            "plum" to RGBColor(221, 160, 221),
            "powderblue" to RGBColor(176, 224, 230),
            "purple" to RGBColor(128, 0, 128),
            "rebeccapurple" to RGBColor(102, 51, 153),
            "red" to RGBColor(255, 0, 0),
            "rosybrown" to RGBColor(188, 143, 143),
            "royalblue" to RGBColor(65, 105, 225),
            "saddlebrown" to RGBColor(139, 69, 19),
            "salmon" to RGBColor(250, 128, 114),
            "sandybrown" to RGBColor(244, 164, 96),
            "seagreen" to RGBColor(46, 139, 87),
            "seashell" to RGBColor(255, 245, 238),
            "sienna" to RGBColor(160, 82, 45),
            "silver" to RGBColor(192, 192, 192),
            "skyblue" to RGBColor(135, 206, 235),
            "slateblue" to RGBColor(106, 90, 205),
            "slategray" to RGBColor(112, 128, 144),
            "snow" to RGBColor(255, 250, 250),
            "springgreen" to RGBColor(0, 255, 127),
            "steelblue" to RGBColor(70, 130, 180),
            "tan" to RGBColor(210, 180, 140),
            "teal" to RGBColor(0, 128, 128),
            "thistle" to RGBColor(216, 191, 216),
            "tomato" to RGBColor(255, 99, 71),
            "turquoise" to RGBColor(64, 224, 208),
            "violet" to RGBColor(238, 130, 238),
            "wheat" to RGBColor(245, 222, 179),
            "white" to RGBColor(255, 255, 255),
            "whitesmoke" to RGBColor(245, 245, 245),
            "yellow" to RGBColor(255, 255, 0),
            "yellowgreen" to RGBColor(154, 205, 50)
        )

        /**
         * Create an RGBColor from a hex string.
         */
        fun fromHex(hexString: String): RGBColor {
            val normalized = Util.normalizeHexString(hexString)
            val r = normalized.substring(0, 2).toInt(16)
            val g = normalized.substring(2, 4).toInt(16)
            val b = normalized.substring(4, 6).toInt(16)
            return RGBColor(r, g, b)
        }

        /**
         * Create an RGBColor from an integer.
         */
        fun fromInt(value: Int): RGBColor {
            val r = (value shr 16) and 0xFF
            val g = (value shr 8) and 0xFF
            val b = value and 0xFF
            return RGBColor(r, g, b)
        }

        /**
         * Get a named color.
         */
        fun fromName(name: String): RGBColor? {
            return namedColors[name.lowercase()]
        }

        /**
         * Parse a color from various formats (name, hex, rgb tuple, etc.).
         */
        fun parse(value: Any): RGBColor {
            return when (value) {
                is RGBColor -> value
                is String -> {
                    // Try named color first
                    fromName(value) ?:
                    // Try hex
                    if (value.startsWith("#") || value.startsWith("0x") || Util.isHexString(value)) {
                        fromHex(value)
                    } else {
                        throw IllegalArgumentException("Unknown color format: $value")
                    }
                }
                is Int -> fromInt(value)
                is List<*> -> {
                    require(value.size >= 3) { "RGB list must have at least 3 elements" }
                    RGBColor(
                        value[0] as? Int ?: (value[0] as Number).toInt(),
                        value[1] as? Int ?: (value[1] as Number).toInt(),
                        value[2] as? Int ?: (value[2] as Number).toInt()
                    )
                }
                else -> throw IllegalArgumentException("Cannot parse color from ${value::class}")
            }
        }

        /**
         * Generate a random color.
         */
        fun random(): RGBColor {
            return RGBColor(
                Random.nextInt(0, 256),
                Random.nextInt(0, 256),
                Random.nextInt(0, 256)
            )
        }
    }
}
