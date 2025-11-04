package org.missionpinball.mpf.core

import mu.KotlinLogging

private val logger = KotlinLogging.logger("Player")

/**
 * Base class for a player in a game.
 *
 * One instance of this class is automatically created for each player.
 *
 * The game mode maintains a `player` attribute which always points to the
 * current player and is available via `machine.game.player`.
 *
 * This Player class is responsible for tracking **player variables** which
 * is a dictionary of key/value pairs maintained on a per-player basis.
 *
 * Every time a player variable is created or changed, an MPF event is posted
 * in the form *player_* plus the variable name. For example, creating or
 * changing the `score` variable will cause an event called *player_score* to
 * be posted.
 *
 * The player variable event will have four parameters posted along with it:
 * - `value` (the new value)
 * - `prev_value` (the old value before it was updated)
 * - `change` (the change in the value)
 * - `player_num` (the player number the variable belongs to)
 */
class Player(
    private val machine: MachineController,
    index: Int
) {
    /**
     * Player variables dictionary.
     */
    private val vars = mutableMapOf<String, Any?>()

    /**
     * Whether events are enabled for this player.
     */
    private var eventsEnabled = false

    /**
     * Player index (0-based).
     */
    val index: Int

    /**
     * Player number (1-based).
     */
    val number: Int

    init {
        this.index = index
        this.number = index + 1

        logger.debug { "Creating new player: Player $number. (player index '$index')" }

        // Set index and number as player variables
        vars["index"] = index
        vars["number"] = number

        // Load initial player vars from config
        loadInitialPlayerVars()

        // Set the initial player score to 0
        set("score", 0)
    }

    /**
     * Load initial player var values from config.
     */
    private fun loadInitialPlayerVars() {
        val playerVarsConfig = machine.config["player_vars"] as? Map<*, *> ?: return

        for ((name, element) in playerVarsConfig) {
            val nameStr = name.toString()
            val elementMap = element as? Map<*, *> ?: continue

            // TODO: Validate config when config_validator is available
            // val validatedElement = machine.configValidator.validateConfig("player_vars", element)

            val initialValue = elementMap["initial_value"]
            val valueType = elementMap["value_type"] as? String ?: "int"

            // Convert initial value to appropriate type
            val convertedValue = when (valueType.lowercase()) {
                "int" -> (initialValue as? Number)?.toInt() ?: 0
                "float", "double" -> (initialValue as? Number)?.toDouble() ?: 0.0
                "str", "string" -> initialValue?.toString() ?: ""
                "bool", "boolean" -> initialValue as? Boolean ?: false
                else -> initialValue
            }

            vars[nameStr] = convertedValue
        }
    }

    /**
     * Enable/disable player variable events.
     *
     * @param enable Flag to enable/disable player variable events.
     * @param sendAllVariables Flag indicating whether or not to send an event
     *                        with the current value of every player variable.
     */
    fun enableEvents(enable: Boolean = true, sendAllVariables: Boolean = true) {
        eventsEnabled = enable

        // Send all current player variable values as events (if requested)
        if (enable && sendAllVariables) {
            sendAllVariableEvents()
        }
    }

    /**
     * Send a player variable event for the current value of all player variables.
     */
    fun sendAllVariableEvents() {
        for ((name, value) in vars) {
            when (value) {
                is Int, is Double, is Float -> sendVariableEvent(name, value, value, 0, number)
                is String -> sendVariableEvent(name, value, value, false, number)
            }
        }
    }

    /**
     * Get a player variable value.
     * Returns 0 if the variable doesn't exist.
     *
     * @param name The variable name.
     * @return The variable value or 0 if it doesn't exist.
     */
    operator fun get(name: String): Any? {
        return vars.getOrDefault(name, 0)
    }

    /**
     * Set a player variable value.
     * Posts a player_<name> event if events are enabled.
     *
     * @param name The variable name.
     * @param value The new value.
     * @param kwargs Additional kwargs to include in the event.
     */
    operator fun set(name: String, value: Any?, vararg kwargs: Pair<String, Any?>) {
        setWithKwargs(name, value, *kwargs)
    }

    /**
     * Set a value to a player variable and include kwargs in the update event.
     *
     * @param name The player variable name.
     * @param value The value to set.
     * @param kwargs Arguments to include in the posted player_<name> event.
     */
    fun setWithKwargs(name: String, value: Any?, vararg kwargs: Pair<String, Any?>) {
        val newEntry = name !in vars
        val prevValue = vars.getOrDefault(name, 0)

        vars[name] = value

        // Calculate change
        val change: Any = try {
            when {
                value is Number && prevValue is Number -> {
                    value.toDouble() - prevValue.toDouble()
                }
                else -> prevValue != value
            }
        } catch (e: Exception) {
            prevValue != value
        }

        // Post event if value changed or is new entry, and is a simple type
        if ((change != 0 && change != false || newEntry) && value is Number || value is String || value is Boolean) {
            logger.debug { "Setting '$name' to: $value, (prior: $prevValue, change: $change)" }

            if (eventsEnabled) {
                sendVariableEvent(name, value, prevValue, change, number, *kwargs)
            }
        }
    }

    /**
     * Add a value to a player variable and include kwargs in the update event.
     *
     * @param name The player variable name.
     * @param value The value to add to the existing value.
     * @param kwargs Arguments to include in the posted player_<name> event.
     */
    fun addWithKwargs(name: String, value: Number, vararg kwargs: Pair<String, Any?>) {
        val current = get(name) as? Number ?: 0
        val newValue = when {
            current is Int && value is Int -> current + value
            else -> current.toDouble() + value.toDouble()
        }
        setWithKwargs(name, newValue, *kwargs)
    }

    /**
     * Send a player variable event and perform any monitor callbacks if configured.
     *
     * @param name The player variable name.
     * @param value The new variable value.
     * @param prevValue The previous variable value.
     * @param change The change in value or True/False.
     * @param playerNum The player number this variable belongs to.
     * @param kwargs Additional kwargs for the event.
     */
    private fun sendVariableEvent(
        name: String,
        value: Any?,
        prevValue: Any?,
        change: Any,
        playerNum: Int,
        vararg kwargs: Pair<String, Any?>
    ) {
        // TODO: Post event when event system is fully integrated
        /*
        machine.events.post(
            "player_$name",
            "value" to value,
            "prev_value" to prevValue,
            "change" to change,
            "player_num" to playerNum,
            *kwargs
        )
        */

        // TODO: Handle monitors when monitor system is available
        /*
        if (monitorEnabled && "player" in machine.monitors) {
            for (callback in machine.monitors["player"]) {
                callback(
                    name = name,
                    value = value,
                    prevValue = prevValue,
                    change = change,
                    playerNum = playerNum
                )
            }
        }
        */
    }

    /**
     * Get all player variables.
     */
    fun getAllVars(): Map<String, Any?> {
        return vars.toMap()
    }

    /**
     * Check if a variable exists.
     */
    fun hasVar(name: String): Boolean {
        return name in vars
    }

    override fun toString(): String {
        return try {
            "<Player ${vars["number"]}>"
        } catch (e: Exception) {
            "<Player (new)>"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Player) return false
        return number == other.number && machine == other.machine
    }

    override fun hashCode(): Int {
        return number.hashCode() xor machine.hashCode()
    }

    companion object {
        /**
         * Class attribute which specifies whether any monitors have been registered
         * to track player variable changes.
         */
        @JvmStatic
        var monitorEnabled = false
    }
}
