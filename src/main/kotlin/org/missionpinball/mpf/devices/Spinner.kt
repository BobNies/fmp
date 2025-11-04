package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Represents a spinner or spinner group in a pinball machine.
 *
 * Spinners track hits from one or more switches and maintain active/idle states.
 * They support event buffering to limit the rate of posted events and can track
 * the number of hits while active.
 *
 * Note: In Python, this uses @DeviceMonitor("active", "idle") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this extends EnableDisableMixinSystemWideDevice.
 * In Kotlin, we implement enable/disable functionality directly.
 */
class Spinner(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "spinners"
    override val collection = "spinners"
    override val classLabel = "spinner"

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Number of hits since the spinner became active.
     */
    var hits = 0
        private set

    /**
     * Whether the spinner is enabled (responds to switch hits).
     */
    var enabled = true

    /**
     * Cached value of active_ms configuration.
     */
    private var activeMs: Long? = null

    /**
     * Whether the spinner is actively spinning (has received hits recently).
     */
    private var _active = false

    /**
     * Whether the spinner is idle (inactive for longer than idle_ms).
     */
    private var _idle = true

    /**
     * Event buffer time in milliseconds to limit event rate.
     */
    private var eventBufferMs: Long? = null

    override suspend fun initialize() {
        super.initialize()

        hits = 0

        // Cache this value because it's used a lot in rapid succession
        activeMs = (config["active_ms"] as? Number)?.toLong()

        val maxEventsPerSecond = (config["max_events_per_second"] as? Number)?.toDouble() ?: 0.0
        if (maxEventsPerSecond > 0) {
            eventBufferMs = ((1 / maxEventsPerSecond) * 1000).toLong()
            debugLog("Configured event buffer for ${eventBufferMs}ms")
        }

        // Can't read the switch until the switch controller is set up
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("init_phase_4", ::registerSwitchHandlers, priority = 1)
        */
    }

    /**
     * Register switch handlers for all configured switches.
     */
    private fun registerSwitchHandlers() {
        val switches = config["switches"] as? List<*> ?: emptyList<Switch>()
        val labels = config["labels"] as? List<*>

        val labelMap = if (labels != null && labels.size == switches.size) {
            switches.zip(labels).toMap()
        } else {
            null
        }

        for (switch in switches) {
            val label = labelMap?.get(switch) as? String

            // TODO: Register switch handler when available
            /*
            val callbackKwargs = if (label != null) mapOf("label" to label) else null
            machine.switchController.addSwitchHandlerObj(
                switch = switch,
                callback = ::updateStateFromSwitch,
                state = 1,
                callbackKwargs = callbackKwargs
            )
            */
        }
    }

    /**
     * Update spinner state when a switch is hit.
     *
     * @param label Optional label for the switch that was hit
     */
    private fun updateStateFromSwitch(label: String? = null) {
        if (!enabled) {
            return
        }

        if (!_active) {
            machine.events.post("spinner_${name}_active", mapOf("label" to label))
            /**
             * Event: spinner_(name)_active
             *
             * The idle spinner became active.
             *
             * This event posts whenever a spinner switch is hit and the spinner
             * is not already active.
             *
             * Args:
             *   label: The label of the switch that triggered the activation
             */

            if (label != null) {
                machine.events.post("spinner_${name}_${label}_active")
                /**
                 * Event: spinner_(name)_(label)_active
                 *
                 * The idle spinner became active on a labeled switch.
                 *
                 * This event posts whenever a spinner switch is hit and the spinner
                 * is not already active, but only if labels are defined.
                 */
            }

            _active = true
            _idle = false
        }

        hits++

        if (eventBufferMs == null || !delay.check("event_buffer")) {
            postHitEvent(label, hits - 1)
        }
    }

    /**
     * Post hit event with current hit count.
     *
     * @param label Optional label for the switch that was hit
     * @param lastHits Hit count from the last event
     */
    private fun postHitEvent(label: String?, lastHits: Int) {
        debugLog("Buffer check has $lastHits previous hits, current is $hits")

        if (lastHits > 0 && lastHits == hits) {
            delay.remove("event_buffer")
            return
        }

        machine.events.post(
            "spinner_${name}_hit",
            mapOf(
                "hits" to hits,
                "change" to (hits - lastHits),
                "label" to label
            )
        )
        /**
         * Event: spinner_(name)_hit
         *
         * The spinner was just hit.
         *
         * This event posts whenever a spinner switch is hit.
         *
         * Args:
         *   hits: The number of switch hits the spinner has had since it became active
         *   change: The number of hits since the last event
         *   label: The label of the switch that was hit
         */

        if (label != null) {
            machine.events.post(
                "spinner_${name}_${label}_hit",
                mapOf(
                    "hits" to hits,
                    "change" to (hits - lastHits)
                )
            )
            /**
             * Event: spinner_(name)_(label)_hit
             *
             * The spinner was just hit on a labeled switch.
             *
             * This event posts whenever a spinner switch is hit and labels
             * are defined for the spinner.
             */
        }

        val activeMsValue = activeMs
        if (activeMsValue != null) {
            delay.reset(activeMsValue, "deactivate") {
                deactivate()
            }
        }

        val eventBufferMsValue = eventBufferMs
        if (eventBufferMsValue != null) {
            delay.add(eventBufferMsValue, "event_buffer") {
                postHitEvent(label, hits)
            }
        }
    }

    /**
     * Post an 'inactive' event after no switch hits for the active_ms duration.
     */
    private fun deactivate() {
        machine.events.post("spinner_${name}_inactive", mapOf("hits" to hits))
        /**
         * Event: spinner_(name)_inactive
         *
         * The spinner is no longer receiving hits.
         *
         * This event posts whenever a spinner has not received hits and
         * its active_ms has timed out.
         *
         * Args:
         *   hits: The number of switch hits the spinner had while it was active
         */

        _active = false

        val idleMs = (config["idle_ms"] as? Number)?.toLong()
        if (idleMs != null) {
            delay.reset(idleMs, "idle") {
                onIdle()
            }
            val resetWhenInactive = config["reset_when_inactive"] as? Boolean ?: false
            if (resetWhenInactive) {
                hits = 0
            }
        } else {
            _idle = true
        }
    }

    /**
     * Post an 'idle' event if the spinner has been inactive for the idle_ms duration.
     */
    private fun onIdle() {
        machine.events.post("spinner_${name}_idle", mapOf("hits" to hits))
        /**
         * Event: spinner_(name)_idle
         *
         * The spinner is now idle.
         *
         * This event posts whenever a spinner has not received hits and
         * its idle_ms has timed out. If no idle_ms is defined, this event
         * will not post.
         *
         * Args:
         *   hits: The number of switch hits the spinner had while it was active
         */

        hits = 0
        _idle = true
    }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        var validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // If single switch is specified, add it to switches list
        val switch = config["switch"]
        val switches = (config["switches"] as? MutableList<Any?>) ?: mutableListOf()

        if (switch != null && switch !in switches) {
            switches.add(switch)
            val mutableConfig = validatedConfig.toMutableMap()
            mutableConfig["switches"] = switches
            validatedConfig = mutableConfig
        }

        // Validate labels count matches switches count
        val labels = config["labels"] as? List<*>
        if (labels != null && labels.size != switches.size) {
            throw IllegalArgumentException(
                "Spinner labels must be the same number as switches. " +
                "Found ${labels.size} labels but ${switches.size} switches."
            )
        }

        return validatedConfig
    }

    /**
     * Whether the spinner is actively spinning.
     */
    val active: Boolean
        get() = _active

    /**
     * Whether the spinner is idle.
     */
    val idle: Boolean
        get() = _idle
}
