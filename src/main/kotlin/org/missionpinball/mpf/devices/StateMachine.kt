package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A generic state machine.
 *
 * State machines define states and transitions between them. Each state
 * can have events that trigger transitions, shows that play while active,
 * and events posted when the state starts or stops.
 *
 * State machines can be system-wide or mode-specific, and can optionally
 * persist their state in player variables.
 *
 * Note: In Python, this uses @DeviceMonitor("state") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class StateMachine(machine: MachineController, name: String) :
    SystemWideDevice(machine, name), ModeDevice(machine, name) {

    override val configSection = "state_machines"
    override val collection = "state_machines"
    override val classLabel = "state_machine"

    /**
     * Current player (when used as mode device).
     */
    var player: Player? = null

    /**
     * Internal state storage (when not persisted to player).
     */
    private var _state: String? = null

    /**
     * Event handler keys for cleanup.
     */
    private val handlers = mutableListOf<Any>()

    /**
     * Currently playing show.
     */
    private var _show: Any? = null

    /**
     * Player variable name for persisting state.
     */
    private val playerVarName = "state_machine_$name"

    override suspend fun deviceAddedSystemWide() {
        super.deviceAddedSystemWide()

        val persistState = config["persist_state"] as? Boolean ?: false
        if (persistState) {
            throw IllegalArgumentException("Cannot set persist_state for system-wide state_machine")
        }

        val startingState = config["starting_state"] as? String ?: ""
        startState(startingState)
    }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        val result = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // Validate transitions reference valid states
        val states = (config["states"] as? Map<*, *>)?.keys?.map { it.toString() }?.toSet() ?: emptySet()
        val transitions = config["transitions"] as? List<*> ?: emptyList<Map<String, Any>>()

        for (transition in transitions) {
            val transitionMap = transition as? Map<*, *> ?: continue

            // Validate source states exist
            val sources = transitionMap["source"] as? List<*> ?: emptyList<String>()
            for (source in sources) {
                val sourceStr = source.toString()
                if (sourceStr !in states) {
                    throw IllegalArgumentException(
                        "Source $sourceStr of transition $transition not found in states."
                    )
                }
            }

            // Validate target state exists
            val target = transitionMap["target"]?.toString()
            if (target != null && target !in states) {
                throw IllegalArgumentException(
                    "Target $target of transition $transition not found in states."
                )
            }
        }

        return result
    }

    override val canExistOutsideOfGame: Boolean
        get() = !(config["persist_state"] as? Boolean ?: false)

    /**
     * Get the current state.
     */
    var state: String?
        get() {
            val persistState = config["persist_state"] as? Boolean ?: false
            return if (persistState && player != null) {
                player?.get(playerVarName) as? String
            } else {
                _state
            }
        }
        set(value) {
            val old = state
            val persistState = config["persist_state"] as? Boolean ?: false

            if (persistState && player != null) {
                player?.set(playerVarName, value)
            } else {
                _state = value
            }

            // Notify monitors
            // TODO: Implement when device monitoring is available
            /*
            notifyVirtualChange("state", old, state)
            */
        }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        this.player = player

        if (state == null) {
            val startingState = config["starting_state"] as? String ?: ""
            startState(startingState)
        } else {
            addHandlersForCurrentState()
            runShowForCurrentState()
            // TODO: Notify when monitoring is available
            /*
            notifyVirtualChange("state", null, state)
            */
        }
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        removeHandlers()
        // TODO: Notify when monitoring is available
        /*
        notifyVirtualChange("state", state, null)
        */
        _state = null
        player = null

        _show?.let { show ->
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
            _show = null
        }
    }

    /**
     * Stop the current state.
     */
    private fun stopCurrentState() {
        val currentState = state ?: return
        debugLog("Stopping state $currentState")

        removeHandlers()

        val states = config["states"] as? Map<*, *> ?: emptyMap<String, Map<String, Any>>()
        val stateConfig = states[currentState] as? Map<*, *> ?: emptyMap<String, Any>()

        val eventsWhenStopped = stateConfig["events_when_stopped"] as? List<*> ?: emptyList<String>()
        for (eventName in eventsWhenStopped) {
            machine.events.post(eventName.toString())
        }

        _show?.let { show ->
            debugLog("Stopping show $show")
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
            _show = null
        }

        state = null
    }

    /**
     * Start a new state.
     *
     * @param stateName Name of the state to start
     */
    private fun startState(stateName: String) {
        debugLog("Starting state $stateName")

        val states = config["states"] as? Map<*, *> ?: emptyMap<String, Map<String, Any>>()
        if (stateName !in states) {
            throw IllegalArgumentException("Invalid state $stateName")
        }

        val stateConfig = states[stateName] as? Map<*, *> ?: emptyMap<String, Any>()
        state = stateName

        val eventsWhenStarted = stateConfig["events_when_started"] as? List<*> ?: emptyList<String>()
        for (eventName in eventsWhenStarted) {
            machine.events.post(eventName.toString())
        }

        addHandlersForCurrentState()
        runShowForCurrentState()
    }

    /**
     * Run the show for the current state (if configured).
     */
    private fun runShowForCurrentState() {
        if (_show != null) {
            throw IllegalStateException("Show already running")
        }

        val currentState = state ?: return
        val states = config["states"] as? Map<*, *> ?: emptyMap<String, Map<String, Any>>()
        val stateConfig = states[currentState] as? Map<*, *> ?: emptyMap<String, Any>()

        val showWhenActive = stateConfig["show_when_active"]
        if (showWhenActive != null) {
            debugLog("Starting show $showWhenActive")
            // TODO: Play show when show controller is available
            /*
            _show = machine.showController.playShowWithConfig(showWhenActive, mode)
            */
        }
    }

    /**
     * Add event handlers for transitions from the current state.
     */
    private fun addHandlersForCurrentState() {
        val currentState = state ?: return
        val transitions = config["transitions"] as? List<*> ?: emptyList<Map<String, Any>>()

        for (transition in transitions) {
            val transitionMap = transition as? Map<*, *> ?: continue
            val sources = transitionMap["source"] as? List<*> ?: emptyList<String>()

            if (currentState in sources.map { it.toString() }) {
                val events = transitionMap["events"] as? List<*> ?: emptyList<String>()
                for (event in events) {
                    // TODO: Register event handler when available
                    /*
                    val handler = machine.events.addHandler(
                        event.toString(),
                        { doTransition(transitionMap) }
                    )
                    handlers.add(handler)
                    */
                }
            }
        }
    }

    /**
     * Perform a state transition.
     *
     * @param transitionConfig Configuration for the transition
     */
    private fun doTransition(transitionConfig: Map<*, *>) {
        val target = transitionConfig["target"]?.toString() ?: return
        infoLog("Transitioning from $state to $target")

        stopCurrentState()

        val eventsWhenTransitioning = transitionConfig["events_when_transitioning"] as? List<*>
            ?: emptyList<String>()
        for (eventName in eventsWhenTransitioning) {
            machine.events.post(eventName.toString())
        }

        startState(target)
    }

    /**
     * Remove all event handlers.
     */
    private fun removeHandlers() {
        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandlersByKeys(handlers)
        */
        handlers.clear()
    }
}
