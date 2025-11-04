package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player

/**
 * An achievement in a pinball machine.
 *
 * Achievements track player progress through various states:
 * - disabled: Cannot be started
 * - enabled: Can be started
 * - started: Currently in progress
 * - stopped: Was started but stopped before completion
 * - completed: Successfully completed
 * - selected: Highlighted for selection (when used with groups)
 *
 * Achievements are tracked per player and can automatically restore state
 * on the next ball.
 *
 * Note: In Python, this uses @DeviceMonitor("state", "selected") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class Achievement(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "achievements"
    override val collection = "achievements"
    override val classLabel = "achievement"

    override val allowEmptyConfigs = true

    /**
     * Current player.
     */
    private var _player: Player? = null

    /**
     * Current mode.
     */
    private var _mode: Mode? = null

    /**
     * Currently playing show.
     */
    private var _show: Any? = null

    /**
     * Get the current state of the achievement.
     */
    var state: String?
        get() {
            return try {
                val achievements = _player?.get("achievements") as? MutableMap<String, List<Any>>
                achievements?.get(name)?.getOrNull(0) as? String
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            try {
                val achievements = _player?.get("achievements") as? MutableMap<String, MutableList<Any>>
                    ?: mutableMapOf<String, MutableList<Any>>().also { map ->
                        _player?.set("achievements", map)
                    }

                if (name in achievements) {
                    achievements[name]!![0] = value ?: "disabled"
                } else {
                    achievements[name] = mutableListOf<Any>(value ?: "disabled", false)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

    /**
     * Whether this achievement can be selected and started.
     */
    val canBeSelectedForStart: Boolean
        get() {
            val currentState = state
            val restartAfterStopPossible = config["restart_after_stop_possible"] as? Boolean ?: false
            return currentState == "enabled" ||
                (currentState == "stopped" && restartAfterStopPossible)
        }

    /**
     * Get the current selection state.
     */
    var selected: Boolean
        get() {
            return try {
                val achievements = _player?.get("achievements") as? Map<String, List<Any>>
                achievements?.get(name)?.getOrNull(1) as? Boolean ?: false
            } catch (e: Exception) {
                false
            }
        }
        set(value) {
            try {
                val achievements = _player?.get("achievements") as? MutableMap<String, MutableList<Any>>
                    ?: mutableMapOf<String, MutableList<Any>>().also { map ->
                        _player?.set("achievements", map)
                    }

                if (name in achievements) {
                    achievements[name]!![1] = value
                } else {
                    achievements[name] = mutableListOf<Any>(null, value)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix).toMutableMap()

        val states = listOf("disabled", "enabled", "started", "stopped", "selected", "completed")

        for (stateValue in states) {
            val eventName = "events_when_$stateValue"
            if (validatedConfig[eventName] == null) {
                validatedConfig[eventName] = listOf("achievement_${name}_state_$stateValue")
            }
        }

        return validatedConfig
    }

    override fun enable() {
        super.enable()
        val currentState = state
        if (currentState in listOf("disabled", "started")) {
            state = "enabled"
            runState()
        }
    }

    /**
     * Event handler for start event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventStart() {
        start()
    }

    /**
     * Start achievement.
     */
    fun start() {
        val currentState = state
        val restartAfterStopPossible = config["restart_after_stop_possible"] as? Boolean ?: false

        if (currentState == "enabled" ||
            (restartAfterStopPossible && currentState == "stopped")) {
            state = "started"
            selected = false
            runState()
        }
    }

    /**
     * Event handler for complete event.
     *
     * TODO: Add @EventHandler(4) annotation when event system is fully integrated
     */
    fun eventComplete() {
        complete()
    }

    /**
     * Complete achievement.
     */
    fun complete() {
        if (state == "started") {
            state = "completed"
            selected = false
            runState()
        }
    }

    /**
     * Event handler for stop event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventStop() {
        stop()
    }

    /**
     * Stop achievement.
     */
    fun stop() {
        if (state == "started") {
            state = "stopped"
            selected = false
            runState()
        }
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(0) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    override fun disable() {
        val currentState = state
        val restartAfterStopPossible = config["restart_after_stop_possible"] as? Boolean ?: false

        if (currentState == "enabled" ||
            (restartAfterStopPossible && currentState == "stopped")) {
            state = "disabled"
            selected = false
            runState()
        }
    }

    /**
     * Event handler for reset event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventReset() {
        reset()
    }

    /**
     * Reset the achievement to its initial state.
     */
    fun reset() {
        // If there is no player active
        if (_player == null) {
            return
        }

        selected = false

        val startEnabled = config["start_enabled"] as? Boolean
        val enableEvents = config["enable_events"]

        state = when {
            startEnabled == true -> "enabled"
            startEnabled == false -> "disabled"
            enableEvents != null -> "disabled"
            else -> "enabled"
        }

        runState()
    }

    /**
     * Event handler for unselect event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventUnselect() {
        unselect()
    }

    /**
     * Remove highlight (unselect) this achievement.
     */
    fun unselect() {
        if (_player == null) {
            return
        }

        debugLog("Unselecting achievement")

        if (selected) {
            selected = false
            runState()
        }
    }

    /**
     * Event handler for select event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventSelect() {
        select()
    }

    /**
     * Highlight (select) this achievement.
     */
    fun select() {
        if (_player == null) {
            return
        }

        debugLog("Selecting achievement")

        val currentState = state
        val restartAfterStopPossible = config["restart_after_stop_possible"] as? Boolean ?: false

        if ((currentState == "enabled" ||
            (restartAfterStopPossible && currentState == "stopped")) && !selected) {
            selected = true
            runState()
        }
    }

    /**
     * Run shows and post events for current state.
     *
     * @param restore Whether this is being called to restore state
     */
    private fun runState(restore: Boolean = false) {
        val currentState = state ?: return
        val isSelected = selected

        machine.events.post(
            "achievement_${name}_changed_state",
            mapOf(
                "restore" to restore,
                "state" to currentState,
                "selected" to isSelected
            )
        )
        /**
         * Event: achievement_(name)_changed_state
         *
         * Achievement (name) changed state.
         *
         * Valid states are: disabled, enabled, started, completed, stopped
         *
         * This is only posted once per state. It's also posted on restart on the next
         * ball to restore state.
         *
         * Args:
         *   restore: true if this is reposted to restore state
         *   state: Current state
         *   selected: Whether this achievement is selected currently
         */

        val events = config["events_when_$currentState"] as? List<*> ?: emptyList<String>()
        for (event in events) {
            machine.events.post(
                event.toString(),
                mapOf(
                    "restore" to restore,
                    "state" to currentState,
                    "selected" to isSelected
                )
            )
        }

        if (isSelected) {
            val selectedEvents = config["events_when_selected"] as? List<*> ?: emptyList<String>()
            for (event in selectedEvents) {
                machine.events.post(
                    event.toString(),
                    mapOf(
                        "restore" to restore,
                        "state" to currentState,
                        "selected" to isSelected
                    )
                )
            }
        }

        // Stop current show
        _show?.let { show ->
            debugLog("Stopping show: $show")
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
            _show = null
        }

        // Start new show
        val showConfig = if (isSelected && config["show_when_selected"] != null) {
            config["show_when_selected"]
        } else {
            config["show_when_$currentState"]
        }

        if (showConfig != null) {
            debugLog("Playing show: $showConfig")
            // TODO: Play show when show controller is available
            /*
            val showTokens = config["show_tokens"] as? Map<String, Any>
            val speed = config["speed"] as? Double ?: 1.0
            val syncMs = config["sync_ms"] as? Long

            _show = showConfig.play(
                priority = _mode?.priority ?: 0,
                loops = -1,
                syncMs = syncMs,
                speed = speed,
                showTokens = showTokens
            )
            */
        }
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        _player = player
        _mode = mode

        // Initialize achievements map if needed
        if (player.get("achievements") == null) {
            player.set("achievements", mutableMapOf<String, MutableList<Any>>())
        }

        val achievements = player.get("achievements") as? Map<String, *>
        if (name !in (achievements?.keys ?: emptySet())) {
            reset()
        } else {
            restoreState()
        }

        // TODO: Notify when monitoring is available
        /*
        notifyVirtualChange("selected", null, state)
        */
    }

    /**
     * Restore state from player variables.
     */
    private fun restoreState() {
        val currentState = state

        val restartOnNextBall = config["restart_on_next_ball_when_started"] as? Boolean ?: false
        val enableOnNextBall = config["enable_on_next_ball_when_enabled"] as? Boolean ?: false

        when {
            currentState == "started" && !restartOnNextBall -> state = "stopped"
            currentState == "enabled" && !enableOnNextBall -> state = "disabled"
            else -> {
                // State might still have changed because of player change
                // TODO: Notify when monitoring is available
                /*
                notifyVirtualChange("state", null, state)
                */
            }
        }

        runState(restore = true)
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        _player = null
        _mode = null

        _show?.let { show ->
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
            _show = null
        }
    }

    override fun addControlEventsInMode(mode: Mode) {
        // Override the default mode device behavior.
        // Achievements use sophisticated logic to handle their mode-starting states
        // during deviceLoadedInMode(). Therefore no default enabling is required.
    }
}
