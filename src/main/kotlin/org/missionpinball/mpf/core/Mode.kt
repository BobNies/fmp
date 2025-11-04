package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.missionpinball.mpf.exceptions.ConfigFileError

private val logger = KotlinLogging.logger {}

/**
 * Base class for a mode.
 *
 * One instance of a mode subclass is created for each mode in the machine.
 * Modes can be started and stopped, have priority levels, and can register
 * event handlers that are automatically removed when the mode stops.
 */
open class Mode(
    val machine: MachineController,
    val config: MutableMap<String, Any?>,
    val name: String,
    val path: String,
    val assetPaths: List<String>
) {
    // Logging functionality via delegation
    private val logMixin: LogMixin = LogMixinImpl()

    protected val log get() = logMixin.log
    protected val debug get() = logMixin.debug

    /**
     * Priority of this mode. Higher priority modes are processed first.
     */
    var priority: Int = 0

    /**
     * Whether this mode is currently active.
     */
    private var _active = false
    val active: Boolean
        get() = _active

    /**
     * Whether this mode is currently starting.
     */
    private var _starting = false
    val starting: Boolean
        get() = _starting

    /**
     * Whether this mode is currently stopping.
     */
    var stopping = false

    /**
     * Wait queue for mode start (if using wait queue).
     */
    private var modeStartWaitQueue: QueuedEvent? = null

    /**
     * Methods to call when mode stops.
     */
    private val stopMethods = mutableListOf<Pair<() -> Unit, Any?>>()

    /**
     * Callback to call when mode finishes starting.
     */
    private var startCallback: (() -> Unit)? = null

    /**
     * Callbacks to call when mode finishes stopping.
     */
    private val stopCallbacks = mutableListOf<() -> Unit>()

    /**
     * Event handlers registered by this mode.
     */
    private val eventHandlers = mutableSetOf<EventHandlerKey>()

    /**
     * Switch handlers registered by this mode.
     */
    private val switchHandlers = mutableListOf<Any>()  // TODO: Type when SwitchHandler is available

    /**
     * Kwargs for mode stop event.
     */
    private var modeStopKwargs = mutableMapOf<String, Any?>()

    /**
     * Devices added by this mode.
     */
    private val modeDevices = mutableSetOf<Device>()

    /**
     * Kwargs for mode start event.
     */
    private var startEventKwargs = mutableMapOf<String, Any?>()

    /**
     * Delay manager for this mode. All delays are automatically canceled when mode stops.
     */
    val delay: DelayManager = DelayManager(machine.clock)

    /**
     * Reference to the current player object.
     */
    var player: Player? = null

    /**
     * Controls whether this mode is stopped when the ball ends.
     */
    var autoStopOnBallEnd: Boolean

    /**
     * Controls whether this mode will restart on the next ball.
     */
    var restartOnNextBall: Boolean

    init {
        // Configure logging
        val modeConfig = config["mode"] as? Map<*, *> ?: emptyMap<String, Any?>()
        logMixin.configureLogging(
            "Mode.$name",
            modeConfig["console_log"] as? String ?: "basic",
            modeConfig["file_log"] as? String ?: "basic"
        )

        // Configure mode settings
        configureModeSettings(modeConfig as? MutableMap<String, Any?> ?: mutableMapOf())

        autoStopOnBallEnd = (modeConfig["stop_on_ball_end"] as? Boolean) ?: true
        restartOnNextBall = (modeConfig["restart_on_next_ball"] as? Boolean) ?: false

        // Validate game mode settings
        val isGameMode = modeConfig["game_mode"] as? Boolean ?: false
        if (isGameMode && !autoStopOnBallEnd) {
            throw ConfigFileError(
                "All game modes need to stop at ball end. If you want to set stop_on_ball_end to " +
                "False also set game_mode to False.",
                1,
                "Mode.$name"
            )
        }
    }

    /**
     * Process this mode's configuration settings from a config dictionary.
     */
    private fun configureModeSettings(modeConfig: MutableMap<String, Any?>) {
        // TODO: Validate config when config_validator is available
        // config["mode"] = machine.configValidator.validateConfig("mode", modeConfig, "mode")

        @Suppress("UNCHECKED_CAST")
        val startEvents = (modeConfig["start_events"] as? List<String>) ?: emptyList()
        val modePriority = (modeConfig["priority"] as? Int) ?: 0
        val startPriority = (modeConfig["start_priority"] as? Int) ?: 0

        for (event in startEvents) {
            // TODO: Add handler when event system is fully integrated
            // machine.events.addHandler(event, ::start, modePriority + startPriority)
        }
    }

    /**
     * Returns true if this is a game mode.
     */
    val isGameMode: Boolean
        get() {
            val modeConfig = config["mode"] as? Map<*, *> ?: emptyMap<String, Any?>()
            return (modeConfig["game_mode"] as? Boolean) ?: false
        }

    /**
     * Start this mode.
     *
     * @param modePriority Integer value of what you want this mode to run at.
     * @param callback Callback to call when this mode has been started.
     * @param kwargs Additional keyword arguments.
     */
    fun start(modePriority: Int? = null, callback: (() -> Unit)? = null, vararg kwargs: Pair<String, Any?>) {
        debugLog("Received request to start")

        // Check if mode can start
        val modeConfig = config["mode"] as? Map<*, *> ?: emptyMap<String, Any?>()
        val isGameMode = (modeConfig["game_mode"] as? Boolean) ?: false
        if (isGameMode && (player == null)) {  // TODO: Also check machine.game
            warningLog("Can only start mode $name during a game. Aborting start.")
            return
        }

        if (_active) {
            debugLog("Mode is already active. Aborting start.")
            return
        }

        if (_starting) {
            debugLog("Mode already starting. Aborting start.")
            return
        }

        _starting = true

        // TODO: Post event when event system is fully integrated
        // machine.events.post("mode_${name}_will_start", *kwargs)

        // Handle wait queue if needed
        val useWaitQueue = (modeConfig["use_wait_queue"] as? Boolean) ?: false
        val queueParam = kwargs.toMap()["queue"]
        if (useWaitQueue && queueParam is QueuedEvent) {
            debugLog("Registering a mode start wait queue")
            modeStartWaitQueue = queueParam
            modeStartWaitQueue?.wait()
        }

        // Set priority
        priority = modePriority ?: ((modeConfig["priority"] as? Int) ?: 0)

        startEventKwargs = kwargs.toMap().toMutableMap()

        // Hook for custom code (called before any mode devices are set up)
        modeWillStart(startEventKwargs)

        addModeDevices()

        debugLog("Registering mode_stop handlers")

        // Register mode stop events
        @Suppress("UNCHECKED_CAST")
        val stopEvents = (modeConfig["stop_events"] as? List<String>) ?: emptyList()
        val stopPriority = (modeConfig["stop_priority"] as? Int) ?: 0

        for (event in stopEvents) {
            addModeEventHandler(event, { _, _ -> stop() }, stopPriority + 1)
        }

        startCallback = callback

        debugLog("Calling mode_start handlers")

        // TODO: Call mode controller start methods when available

        setupDeviceControlEvents()

        // TODO: Post queue event when event system is fully integrated
        // machine.events.postQueue("mode_${name}_starting", ::started, *kwargs.toList().toTypedArray())

        // For now, directly call started
        started()
    }

    /**
     * Called when mode has finished starting (after queue event completes).
     */
    private fun started(vararg kwargs: Pair<String, Any?>) {
        if (machine.isShuttingDown) {
            infoLog("Will not start because machine is shutting down.")
            return
        }

        infoLog("Started. Priority: $priority")

        _active = true
        _starting = false

        val modeConfig = config["mode"] as? Map<*, *> ?: emptyMap<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        val eventsWhenStarted = (modeConfig["events_when_started"] as? List<String>) ?: emptyList()
        for (eventName in eventsWhenStarted) {
            // TODO: Post event when event system is fully integrated
            // machine.events.post(eventName)
        }

        // TODO: Post started event when event system is fully integrated
        // machine.events.post("mode_${name}_started", ::modeStartedCallback, *startEventKwargs.toList().toTypedArray())

        // For now, directly call callback
        modeStartedCallback()
    }

    /**
     * Called after mode_started event completes.
     */
    private fun modeStartedCallback(vararg kwargs: Pair<String, Any?>) {
        modeStart(startEventKwargs)

        startEventKwargs.clear()

        startCallback?.invoke()

        debugLog("Mode Start process complete.")
    }

    /**
     * Stop this mode.
     *
     * @param callback Method to call when mode has stopped.
     * @param kwargs Additional keyword arguments.
     * @return True if the mode is running, false otherwise.
     */
    fun stop(callback: (() -> Unit)? = null, vararg kwargs: Pair<String, Any?>): Boolean {
        if (!_active) {
            return false
        }

        if (callback != null) {
            stopCallbacks.add(callback)
        }

        // Don't stop twice - only register callback in that case
        if (stopping) {
            return true
        }

        // TODO: Post event when event system is fully integrated
        // machine.events.post("mode_${name}_will_stop")

        stopping = true
        modeStopKwargs = kwargs.toMap().toMutableMap()

        debugLog("Mode Stopping.")

        removeModeSwitch Handlers()
        delay.reset()

        // TODO: Post queue event when event system is fully integrated
        // machine.events.postQueue("mode_${name}_stopping", ::stopped)

        // For now, directly call stopped
        stopped()

        return true
    }

    /**
     * Called when mode has finished stopping (after queue event completes).
     */
    private fun stopped() {
        infoLog("Stopped.")

        priority = 0
        _active = false
        stopping = false

        for ((stopMethod, _) in stopMethods) {
            stopMethod()
        }
        stopMethods.clear()

        val modeConfig = config["mode"] as? Map<*, *> ?: emptyMap<String, Any?>()

        @Suppress("UNCHECKED_CAST")
        val eventsWhenStopped = (modeConfig["events_when_stopped"] as? List<String>) ?: emptyList()
        for (eventName in eventsWhenStopped) {
            // TODO: Post event when event system is fully integrated
            // machine.events.post(eventName)
        }

        // TODO: Post stopped event when event system is fully integrated
        // machine.events.post("mode_${name}_stopped", ::modeStoppedCallback)
        // machine.events.post("clear", "key" to name)

        // For now, directly call callback
        modeStoppedCallback()

        modeStartWaitQueue?.clear()
        modeStartWaitQueue = null
    }

    /**
     * Called after mode_stopped event completes.
     */
    private fun modeStoppedCallback() {
        // Call the mode_stop() method before removing the devices
        modeStop(modeStopKwargs)
        modeStopKwargs.clear()

        // Clean up the mode handlers and devices
        removeModeEventHandlers()
        removeModeDevices()

        for (callback in stopCallbacks) {
            callback()
        }
        stopCallbacks.clear()
    }

    /**
     * Add and initialize mode devices which get removed at the end of the mode.
     */
    private fun addModeDevices() {
        // TODO: Implement when device collections are available on machine
        /*
        for ((configKey, deviceConfig) in config) {
            if (configKey !in machine.config["mpf"]["device_modules"]) {
                continue
            }

            val collection = getattr(machine, configKey) as? DeviceCollection<Device>
            for ((deviceName, _) in deviceConfig) {
                val device = collection[deviceName]
                modeDevices.add(device)

                // This lets the device know it was added to a mode
                device.deviceLoadedInMode(this, player)
            }
        }
        */
    }

    /**
     * Remove mode devices.
     */
    private fun removeModeDevices() {
        for (device in modeDevices) {
            // TODO: Call device.deviceRemovedFromMode(this) when available
        }
        modeDevices.clear()
    }

    /**
     * Setup device control events for this mode.
     */
    private fun setupDeviceControlEvents() {
        debugLog("Scanning mode-based config for device control_events")

        // TODO: Implement when device manager is fully integrated
        /*
        for (controlEvent in machine.deviceManager.getDeviceControlEvents(config)) {
            if (controlEvent.delay <= 0) {
                addModeEventHandler(
                    controlEvent.event,
                    { _, _ -> controlEvent.method() },
                    blockingFacility = controlEvent.device.classLabel
                )
            } else {
                addModeEventHandler(
                    controlEvent.event,
                    { _, _ -> controlEventHandler(controlEvent.method, controlEvent.delay) },
                    blockingFacility = controlEvent.device.classLabel
                )
            }
        }
        */
    }

    /**
     * Handler for delayed control events.
     */
    private fun controlEventHandler(callback: () -> Unit, msDelay: Int) {
        debugLog("_control_event_handler: callback: $callback")
        delay.add(msDelay.toLong(), callback)
    }

    /**
     * Register an event handler which is automatically removed when this mode stops.
     *
     * @param event String name of the event you're adding a handler for.
     * @param handler The method that will be called when the event is fired.
     * @param priorityOffset Offset to add to mode priority.
     * @param blockingFacility Optional blocking facility.
     * @param kwargs Additional keyword arguments.
     * @return EventHandlerKey to the handler.
     */
    fun addModeEventHandler(
        event: String,
        handler: (Map<String, Any?>, Map<String, Any?>) -> Any?,
        priorityOffset: Int = 0,
        blockingFacility: Any? = null,
        vararg kwargs: Pair<String, Any?>
    ): EventHandlerKey {
        // Convert to simple callback for now
        val simpleHandler: (Map<String, Any?>) -> Any? = { params ->
            handler(params, kwargs.toMap())
        }

        // TODO: Add handler when event system is fully integrated with mode support
        // val key = machine.events.addHandler(event, simpleHandler, priority + priorityOffset, blockingFacility, *kwargs, "mode" to this)

        // Placeholder key
        val key = EventHandlerKey(java.util.UUID.randomUUID(), event)

        eventHandlers.add(key)
        return key
    }

    /**
     * Remove all event handlers registered by this mode.
     */
    private fun removeModeEventHandlers() {
        for (key in eventHandlers) {
            // TODO: Remove handler when event system is fully integrated
            // machine.events.removeHandlerByKey(key)
        }
        eventHandlers.clear()
    }

    /**
     * Remove all switch handlers registered by this mode.
     */
    private fun removeModeSwitch Handlers() {
        // TODO: Implement when switch controller is available
        switchHandlers.clear()
    }

    // ==== Lifecycle hooks - override in subclasses ====

    /**
     * Called before mode starts (before devices are added).
     * Override in subclasses.
     */
    open fun modeWillStart(kwargs: Map<String, Any?>) {
        // Subclasses can override
    }

    /**
     * Called when mode starts (after all initialization).
     * Override in subclasses.
     */
    open fun modeStart(kwargs: Map<String, Any?>) {
        // Subclasses can override
    }

    /**
     * Called when mode stops (before cleanup).
     * Override in subclasses.
     */
    open fun modeStop(kwargs: Map<String, Any?>) {
        // Subclasses can override
    }

    /**
     * Called after mode is fully initialized.
     * Override in subclasses.
     */
    open fun initializeMode() {
        // Subclasses can override
    }

    // ==== Utility methods ====

    override fun toString(): String {
        return "<Mode.$name>"
    }

    // Delegate logging methods to logMixin
    protected fun debugLog(msg: String, vararg args: Any?) {
        logMixin.debugLog(msg, *args)
    }

    protected fun infoLog(msg: String, vararg args: Any?) {
        logMixin.infoLog(msg, *args)
    }

    protected fun warningLog(msg: String, vararg args: Any?) {
        logMixin.warningLog(msg, *args)
    }

    protected fun errorLog(msg: String, vararg args: Any?) {
        logMixin.errorLog(msg, *args)
    }

    companion object {
        /**
         * Get config spec for mode_settings.
         * Override in subclasses that need custom config specs.
         */
        @JvmStatic
        fun getConfigSpec(): Map<String, Any> {
            return mapOf(
                "__valid_in__" to "mode",
                "__allow_others__" to ""
            )
        }
    }
}
