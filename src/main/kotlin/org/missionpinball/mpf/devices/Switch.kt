package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.Device
import org.missionpinball.mpf.core.MachineController
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A switch in a pinball machine.
 *
 * Tracks switch state (active/inactive) and posts events when the state changes.
 * Supports NC (normally closed) and NO (normally open) switch types.
 */
class Switch(machine: MachineController, name: String) : Device(machine, name) {

    override val configSection = "switches"
    override val collection = "switches"
    override val classLabel = "switch"

    /**
     * Hardware switch interface.
     * TODO: Implement when platform interfaces are available.
     */
    var hwSwitch: Any? = null

    /**
     * The logical state of the switch.
     * 1 = active, 0 = inactive.
     * This takes into consideration the NC or NO settings for the switch.
     */
    var state: Int = 0

    /**
     * The physical hardware state of the switch.
     * 1 = active, 0 = inactive.
     * This is what the actual hardware is reporting and does not consider
     * whether a switch is NC or NO.
     */
    var hwState: Int = 0

    /**
     * Whether this switch is inverted (NC switch).
     * 1 = inverted, 0 = not inverted.
     */
    var invert: Int = 0

    /**
     * Recycle seconds - debounce/ignore window.
     */
    private var recycleSecs: Double = 0.0

    /**
     * Time when recycle window ends.
     */
    private var recycleClearTime: Long? = null

    /**
     * Count of jittered events during recycle.
     */
    var recycleJitterCount: Int = 0

    /**
     * Timestamp of last state change.
     */
    private var lastChange: Long = -100000

    /**
     * Events to post for each state (0 = inactive, 1 = active).
     */
    private val eventsToPost: MutableMap<Int, MutableList<String>> = mutableMapOf(
        0 to mutableListOf(),
        1 to mutableListOf()
    )

    /**
     * Sources that have muted this switch.
     */
    private val mutes = mutableSetOf<String>()

    /**
     * Whether this switch is currently muted.
     */
    val isMuted: Boolean
        get() = mutes.isNotEmpty()

    init {
        // Register switch with switch controller
        // TODO: Uncomment when switch controller is available
        // machine.switchController.registerSwitch(this)
    }

    /**
     * Get milliseconds since last state change.
     *
     * @param currentTime Current time in ms, or null to use current clock time.
     * @return Milliseconds since last change.
     */
    fun getMsSinceLastChange(currentTime: Long? = null): Long {
        val time = currentTime ?: machine.clock.getTime()
        return time - lastChange
    }

    override suspend fun initialize() {
        super.initialize()

        // TODO: Get platform when platform system is available
        // platform = machine.getPlatformSections("switches", config["platform"])

        // Check if this is an NC (normally closed) switch
        val switchType = config["type"] as? String ?: "NO"
        if (switchType.uppercase() == "NC") {
            invert = 1
        }

        // Set up debounce/recycle window
        val ignoreWindowMs = (config["ignore_window_ms"] as? Number)?.toLong() ?: 0
        recycleSecs = ignoreWindowMs / 1000.0

        // TODO: Configure hardware switch when platform is available
        /*
        val switchConfig = SwitchConfig(
            name = name,
            invert = invert,
            debounce = config["debounce"] as? Boolean ?: false
        )
        hwSwitch = platform.configureSwitch(config["number"], switchConfig, config["platform_settings"])
        */

        // Add handlers for posting events
        if (recycleSecs > 0) {
            addHandler(callback = ::postEventsWithRecycle, state = 1, callbackKwargs = mapOf("state" to 1))
            addHandler(callback = ::postEventsWithRecycle, state = 0, callbackKwargs = mapOf("state" to 0))
        } else {
            addHandler(callback = ::postEvents, state = 1, callbackKwargs = mapOf("state" to 1))
            addHandler(callback = ::postEvents, state = 0, callbackKwargs = mapOf("state" to 0))
        }

        // Auto-create switch events if configured
        val autoCreateEvents = machine.config["mpf"]?.let { it as? Map<*, *> }
            ?.get("auto_create_switch_events") as? Boolean ?: true

        if (autoCreateEvents) {
            val switchEventActive = machine.config["mpf"]?.let { it as? Map<*, *> }
                ?.get("switch_event_active") as? String ?: "%_active"
            val switchEventInactive = machine.config["mpf"]?.let { it as? Map<*, *> }
                ?.get("switch_event_inactive") as? String ?: "%_inactive"

            createActivationEvent(switchEventActive.replace("%", name), 1)
            createActivationEvent(switchEventInactive.replace("%", name), 0)
        }

        // Create events for tags
        val switchTagEvent = machine.config["mpf"]?.let { it as? Map<*, *> }
            ?.get("switch_tag_event") as? String ?: "sw_%"

        for (tag in tags) {
            createActivationEvent(switchTagEvent.replace("%", tag), 1)
            createActivationEvent("${switchTagEvent.replace("%", tag)}_active", 1)
            createActivationEvent("${switchTagEvent.replace("%", tag)}_inactive", 0)
        }

        // Create custom events from config
        val eventsWhenActivated = config["events_when_activated"] as? List<*> ?: emptyList<String>()
        for (event in eventsWhenActivated) {
            createActivationEvent(event.toString(), 1)
        }

        val eventsWhenDeactivated = config["events_when_deactivated"] as? List<*> ?: emptyList<String>()
        for (event in eventsWhenDeactivated) {
            createActivationEvent(event.toString(), 0)
        }

        // Handle ball search muting
        val ignoreDuringBallSearch = config["ignore_during_ball_search"] as? Boolean ?: false
        if (ignoreDuringBallSearch) {
            // TODO: Add handlers when event system is fully integrated
            // machine.events.addHandler("ball_search_started", ::mute, "source" to "ball_search")
            // machine.events.addHandler("ball_search_stopped", ::unmute, "source" to "ball_search")
        }
    }

    /**
     * Create an activation event for this switch.
     *
     * @param eventStr Event string, optionally with delay (e.g., "event|100ms")
     * @param state State (0 or 1) that triggers this event.
     */
    private fun createActivationEvent(eventStr: String, state: Int) {
        if ("|" in eventStr) {
            val parts = eventStr.split("|")
            val event = parts[0]
            val evTime = parts.getOrNull(1) ?: "0ms"

            // Parse time to milliseconds
            val ms = parseTimeToMs(evTime)

            // TODO: Add switch handler when switch controller is available
            /*
            machine.switchController.addSwitchHandlerObj(
                switch = this,
                state = state,
                callback = { machine.events.post(event) },
                ms = ms
            )
            */
        } else {
            eventsToPost[state]?.add(eventStr)
        }
    }

    /**
     * Parse time string to milliseconds.
     */
    private fun parseTimeToMs(timeStr: String): Long {
        val cleaned = timeStr.trim().lowercase()
        return when {
            cleaned.endsWith("ms") -> cleaned.removeSuffix("ms").toLongOrNull() ?: 0
            cleaned.endsWith("s") -> (cleaned.removeSuffix("s").toDoubleOrNull() ?: 0.0).let { (it * 1000).toLong() }
            else -> cleaned.toLongOrNull() ?: 0
        }
    }

    /**
     * Post events with recycle window handling.
     */
    private fun postEventsWithRecycle(kwargs: Map<String, Any?>) {
        val state = kwargs["state"] as? Int ?: return

        // If recycle is not active, start it
        if (recycleClearTime == null) {
            recycleClearTime = lastChange + (recycleSecs * 1000).toLong()

            // TODO: Schedule recycle clear when clock integration is complete
            // machine.clock.scheduleOnce({ recyclePassed(state) }, recycleSecs)

            postEvents(kwargs)
        }
    }

    /**
     * Called when recycle window passes.
     */
    private fun recyclePassed(state: Int) {
        recycleClearTime = null
        // Only post event if the switch toggled during recycle
        if (this.state != state) {
            postEvents(mapOf("state" to this.state))
        }
    }

    /**
     * Post events for the given state.
     */
    private fun postEvents(kwargs: Map<String, Any?>) {
        val state = kwargs["state"] as? Int ?: return

        for (event in eventsToPost[state] ?: emptyList()) {
            // TODO: Post event when event system is fully integrated
            // if (debug || machine.events.doesEventExist(event)) {
            //     machine.events.post(event)
            // }
            debugLog("Would post event: $event")
        }
    }

    /**
     * Add a switch handler (callback) for this switch.
     *
     * @param callback Method to call when switch state changes.
     * @param state State (0 or 1) that triggers the callback.
     * @param ms Milliseconds the switch must be in state before callback.
     * @param returnInfo Whether to pass switch info to callback.
     * @param callbackKwargs Additional kwargs for callback.
     * @return Handler key for later removal.
     */
    fun addHandler(
        callback: (Map<String, Any?>) -> Unit,
        state: Int = 1,
        ms: Long = 0,
        returnInfo: Boolean = false,
        callbackKwargs: Map<String, Any?>? = null
    ): Any {
        // TODO: Implement when switch controller is available
        // return machine.switchController.addSwitchHandlerObj(
        //     this, callback, state, ms, returnInfo, callbackKwargs
        // )
        return Any()
    }

    /**
     * Remove a switch handler.
     *
     * @param callback The callback to remove.
     * @param state The state to remove handler for.
     * @param ms The ms delay to match.
     */
    fun removeHandler(callback: (Map<String, Any?>) -> Unit, state: Int = 1, ms: Long = 0) {
        // TODO: Implement when switch controller is available
        // machine.switchController.removeSwitchHandlerObj(this, callback, state, ms)
    }

    /**
     * Mute this switch so that hits do not trigger handlers.
     *
     * @param source Source identifier for the mute.
     */
    fun mute(source: String, vararg kwargs: Pair<String, Any?>) {
        mutes.add(source)
        debugLog("Switch muted by $source")
    }

    /**
     * Unmute this switch so that hits again trigger handlers.
     *
     * @param source Source identifier for the unmute.
     */
    fun unmute(source: String, vararg kwargs: Pair<String, Any?>) {
        mutes.remove(source)
        debugLog("Switch unmuted by $source")
    }

    /**
     * Get the playfield this switch is assigned to.
     */
    val playfield: Any?
        get() = config["playfield"]

    companion object {
        /**
         * Device class initialization.
         * Register handler for duplicate switch number checks.
         */
        @JvmStatic
        override fun deviceClassInit(machine: MachineController) {
            // TODO: Add handler when event system is fully integrated
            // machine.events.addHandler("init_phase_4", ::checkDuplicateSwitchNumbers, "machine" to machine)
        }

        /**
         * Check for duplicate switch numbers.
         */
        @JvmStatic
        private fun checkDuplicateSwitchNumbers(machine: MachineController) {
            val checkSet = mutableSetOf<Pair<String?, Any?>>()

            // TODO: Implement when switch collection is available
            /*
            for (switch in machine.switches.values) {
                val key = Pair(switch.config["platform"] as? String, switch.hwSwitch?.number)
                if (key in checkSet) {
                    throw IllegalStateException(
                        "Duplicate switch number ${switch.hwSwitch?.number} for switch $switch"
                    )
                }
                checkSet.add(key)
            }
            */
        }
    }
}
