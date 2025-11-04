package org.missionpinball.mpf.core

/**
 * Utility functions for MPF.
 */
object Util {
    private val hexMatcher = Regex("[a-fA-F0-9]{6,8}")

    /**
     * Convert value to a simple type (primitives, strings, lists, maps).
     */
    fun convertToSimpleType(value: Any?): Any? {
        return when (value) {
            null -> null
            is Int, is String, is Float, is Double, is Boolean -> value
            is List<*> -> value.map { convertToSimpleType(it) }
            is Map<*, *> -> value.mapKeys { convertToSimpleType(it.key) }
                .mapValues { convertToSimpleType(it.value) }
            is Triple<*, *, *> -> Triple(
                convertToSimpleType(value.first),
                convertToSimpleType(value.second),
                convertToSimpleType(value.third)
            )
            // For RGBColor - we'll handle this when RGBColor is implemented
            else -> value.toString()
        }
    }

    /**
     * Convert value to a specific type.
     */
    fun convertToType(value: String, typeName: String): Any {
        return when (typeName.lowercase()) {
            "int" -> value.toInt()
            "float" -> value.toFloat()
            "double" -> value.toDouble()
            "str", "string" -> value
            "bool", "boolean" -> value.toBoolean()
            else -> throw IllegalArgumentException("Unknown type $typeName")
        }
    }

    /**
     * Convert the keys of a dictionary to lowercase.
     */
    fun keysToLower(source: Any?): Any? {
        return when (source) {
            null -> emptyMap<String, Any?>()
            is Map<*, *> -> source.mapKeys { (k, _) ->
                k.toString().lowercase()
            }.mapValues { (_, v) ->
                keysToLower(v)
            }
            is List<*> -> source.map { keysToLower(it) }
            is Int, is Float, is Double, is String, is Boolean -> source
            else -> throw IllegalArgumentException("Source of type ${source::class} has invalid format")
        }
    }

    /**
     * Convert a comma-separated string into a list if not already a list.
     */
    fun stringToList(string: Any?): List<Any?> {
        return when (string) {
            is String -> {
                if (string.isEmpty()) {
                    emptyList()
                } else {
                    string.split(",").map { it.trim().ifEmpty { null } }
                }
            }
            is List<*> -> string
            null -> emptyList()
            is Int, is Float, is Double -> listOf(string)
            else -> throw IllegalArgumentException("Incorrect type in list for element $string")
        }
    }

    /**
     * Convert a comma-separated and/or space-separated event string into a list.
     * This version honors placeholders/templates for events.
     */
    fun stringToEventList(string: Any?): List<String?> {
        return when (string) {
            is String -> {
                if (string.isEmpty()) {
                    emptyList()
                } else if ("{" in string) {
                    // Split the string on spaces/commas EXCEPT regions within braces
                    Regex("""([\w|-]+?\{.*?\}|[\w|-]+)""").findAll(string)
                        .map { it.value.trim() }
                        .map { if (it == "none") null else it }
                        .toList()
                } else {
                    // Split at commas
                    string.split(",")
                        .map { it.trim() }
                        .map { if (it == "none") null else it }
                }
            }
            is List<*> -> string.map { it?.toString() }
            null -> emptyList()
            else -> throw IllegalArgumentException("stringToEventList got $string which is type ${string::class}")
        }
    }

    /**
     * Convert a list to a comma-separated string.
     */
    fun listToString(inputList: List<Any?>): String {
        return inputList.joinToString(", ")
    }

    /**
     * Chunk a list into sublists of a specific size.
     */
    fun <T> chunker(seq: List<T>, size: Int): List<List<T>> {
        return seq.chunked(size)
    }

    /**
     * Check if a hex string is valid.
     */
    fun isHexString(hexString: String): Boolean {
        return hexMatcher.matches(hexString)
    }

    /**
     * Normalize hex string (ensure it's lowercase and 6 or 8 characters).
     */
    fun normalizeHexString(hexString: String): String {
        var hex = hexString.lowercase().trim()
        if (hex.startsWith("#")) {
            hex = hex.substring(1)
        }
        if (hex.startsWith("0x")) {
            hex = hex.substring(2)
        }
        if (hex.length == 6 || hex.length == 8) {
            return hex
        }
        throw IllegalArgumentException("Invalid hex string: $hexString")
    }

    /**
     * Convert milliseconds to seconds (float).
     */
    fun msToSecs(ms: Int): Double {
        return ms / 1000.0
    }

    /**
     * Convert seconds to milliseconds (int).
     */
    fun secsToMs(secs: Double): Long {
        return (secs * 1000).toLong()
    }

    /**
     * Power function using repeated multiplication (for integer exponents).
     */
    fun power(base: Int, exp: Int): Int {
        return when {
            exp == 0 -> 1
            exp == 1 -> base
            exp > 1 -> (2..exp).fold(base) { acc, _ -> acc * base }
            else -> throw IllegalArgumentException("Negative exponents not supported")
        }
    }

    /**
     * Get first element of a list or null if empty.
     */
    fun <T> firstOrNull(list: List<T>): T? {
        return list.firstOrNull()
    }

    /**
     * Clamp a value between min and max.
     */
    fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
}
