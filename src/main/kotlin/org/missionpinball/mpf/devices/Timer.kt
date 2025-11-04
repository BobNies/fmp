package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player

/**
 * Parent class for a mode timer.
 *
 * Timers can count up or down, have configurable tick intervals, and post
 * events on start, stop, pause, complete, and each tick. They support
 * control events for dynamic manipulation during gameplay.
 *
 * Note: In Python, this uses @DeviceMonitor("running", "ticks", "end_value", "max_value", "start_value") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Timer(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "timers"
    override val collection = "timers"
    override val classLabel = "timer"

    /**
     * Whether the timer is currently running.
     */
    var running = false

    /**
     * Starting value for the timer.
     */
    var startValue: Int? = null

    /**
     * Whether to restart when complete.
     */
    var restartOnComplete: Boolean? = null

    /**
     * Current tick count.
     */
    private var _ticks = 0

    /**
     * Player variable name for tick storage.
     */
    private var tickVar: String? = null

    /**
     * Seconds per tick.
     */
    var tickSecs: Double? = null

    /**
     * Current player.
     */
    var player: Player? = null

    /**
     * End value (when timer completes).
     */
    var endValue: Int? = null

    /**
     * Maximum value the timer can reach.
     */
    var maxValue: Int? = null

    /**
     * Ticks remaining until end.
     */
    var ticksRemaining: Int? = null

    /**
     * Direction ("up" or "down").
     */
    var direction: String? = null

    /**
     * Periodic timer task.
     */
    private var timer: Any? = null

    /**
     * Event handler keys for cleanup.
     */
    private val eventKeys = mutableListOf<Any>()

    /**
     * Delay manager for timing operations.
     */
    private var delay: DelayManager? = null

    override suspend fun deviceAddedToMode(mode: Mode) {
        super.deviceAddedToMode(mode)
        tickVar = "${mode.name}_${name}_tick"
    }

    override suspend fun initialize() {
        super.initialize()

        ticksRemaining = 0
        maxValue = config["max_value"] as? Int
        direction = config["direction"] as? String ?: "down"
        tickSecs = null
        timer = null
        eventKeys.clear()
        delay = DelayManager(machine.clock)

        restartOnComplete = config["restart_on_complete"] as? Boolean ?: false
        endValue = null
        startValue = null
        ticks = 0

        debugLog("----------- Initial Values -----------")
        debugLog("running: $running")
        debugLog("start_value: $startValue")
        debugLog("restart_on_complete: $restartOnComplete")
        debugLog("_ticks: $ticks")
        debugLog("end_value: $endValue")
        debugLog("ticks_remaining: $ticksRemaining")
        debugLog("max_value: $maxValue")
        debugLog("direction: $direction")
        debugLog("tick_secs: $tickSecs")
        debugLog("--------------------------------------")
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        this.player = player

        tickSecs = (config["tick_interval"] as? Number)?.toDouble() ?: 1.0

        try {
            endValue = (config["end_value"] as? Number)?.toInt()
        } catch (e: Exception) {
            endValue = null
        }

        if (direction == "down" && endValue == null) {
            endValue = 0  // Need it to be 0 not null
        }

        startValue = (config["start_value"] as? Number)?.toInt() ?: 0
        ticks = startValue ?: 0

        val controlEvents = config["control_events"] as? List<*>
        if (controlEvents != null) {
            setupControlEvents(controlEvents)
        }

        val startRunning = config["start_running"] as? Boolean ?: false
        if (startRunning) {
            start()
        }
    }

    /**
     * Current tick count.
     */
    var ticks: Int
        get() = _ticks
        set(value) {
            _ticks = value
            try {
                player?.set(tickVar ?: "", value)
                /**
                 * Player variable: (mode)_(timer)_tick
                 *
                 * Stores the current tick value for the timer from the mode
                 * with the timer name. For example, a timer called "my_timer"
                 * which is in the config for "mode1" will store its tick value
                 * in the player variable mode1_my_timer_tick.
                 */
            } catch (e: Exception) {
                // Ignore
            }
        }

    override val canExistOutsideOfGame: Boolean
        get() = true

    /**
     * Setup control events.
     */
    private fun setupControlEvents(eventList: List<*>) {
        debugLog("Setting up control events")

        for (entry in eventList) {
            val entryMap = entry as? Map<*, *> ?: continue
            val action = entryMap["action"] as? String ?: continue
            val event = entryMap["event"] as? String ?: continue
            val value = entryMap["value"]

            // TODO: Register event handlers when available
            /*
            when (action) {
                "add", "subtract", "jump", "set_tick_interval" -> {
                    val handler = when (action) {
                        "add" -> ::add
                        "subtract" -> ::subtract
                        "jump" -> ::jump
                        "set_tick_interval" -> ::setTickInterval
                        else -> continue
                    }
                    val key = machine.events.addHandler(event, handler, timerValue = value)
                    eventKeys.add(key)
                }
                "start", "stop", "reset", "restart" -> {
                    val handler = when (action) {
                        "start" -> ::start
                        "stop" -> ::stop
                        "reset" -> ::reset
                        "restart" -> ::restart
                        else -> continue
                    }
                    val key = machine.events.addHandler(event, handler)
                    eventKeys.add(key)
                }
                "change_tick_interval" -> {
                    val key = machine.events.addHandler(event, ::changeTickInterval, change = value)
                    eventKeys.add(key)
                }
                "reset_tick_interval" -> {
                    val key = machine.events.addHandler(event, ::setTickInterval,
                        timerValue = config["tick_interval"])
                    eventKeys.add(key)
                }
                else -> throw IllegalArgumentException("Invalid control_event action $action")
            }
            */
        }
    }

    /**
     * Remove control events.
     */
    private fun removeControlEvents() {
        debugLog("Removing control events")
        // TODO: Remove event handlers when available
        /*
        for (key in eventKeys) {
            machine.events.removeHandlerByKey(key)
        }
        */
    }

    /**
     * Reset this timer to the starting value.
     */
    fun reset() {
        debugLog("Resetting timer. New value: $startValue")
        jump(startValue ?: 0)
    }

    /**
     * Start this timer.
     */
    fun start() {
        // Do not start if timer is already running
        if (running) {
            return
        }

        infoLog("Starting Timer.")

        if (checkForDone()) {
            return
        }

        running = true

        delay?.remove("pause")
        createSystemTimer()

        machine.events.post(
            "timer_${name}_started",
            mapOf(
                "ticks" to ticks,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_started
         *
         * The timer named (name) has just started.
         *
         * Args:
         *   ticks: The current tick number this timer is at
         *   ticks_remaining: The number of ticks in this timer remaining
         */

        postTickEvents()
    }

    /**
     * Restart the timer by resetting it and then starting it.
     */
    fun restart() {
        reset()
        // If the timer is not running, start it
        if (!running) {
            start()
        } else {
            // If the timer is running, post an updated tick event
            postTickEvents()
        }
    }

    /**
     * Stop the timer.
     */
    fun stop() {
        infoLog("Stopping Timer")

        delay?.remove("pause")

        running = false
        removeSystemTimer()

        machine.events.post(
            "timer_${name}_stopped",
            mapOf(
                "ticks" to ticks,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_stopped
         *
         * The timer named (name) has stopped.
         *
         * This event is posted any time the timer stops, whether it stops because
         * it ended or because it was stopped early by some other event.
         *
         * Args:
         *   ticks: The current tick number this timer is at
         *   ticks_remaining: The number of ticks in this timer remaining
         */
    }

    /**
     * Pause the timer.
     *
     * @param timerValue How many seconds to pause for (real-world seconds)
     */
    fun pause(timerValue: Int = 0) {
        val pauseMs = if (timerValue > 0) timerValue * 1000L else 0L

        infoLog("Pausing Timer for $pauseMs ms")

        running = false

        removeSystemTimer()
        machine.events.post(
            "timer_${name}_paused",
            mapOf(
                "ticks" to ticks,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_paused
         *
         * The timer named (name) has paused.
         *
         * Args:
         *   ticks: The current tick number this timer is at
         *   ticks_remaining: The number of ticks in this timer remaining
         */

        if (pauseMs > 0) {
            delay?.add(pauseMs, "pause") {
                start()
            }
        }
    }

    /**
     * Called when this timer completes.
     */
    fun timerComplete() {
        infoLog("Timer Complete")

        stop()

        machine.events.post(
            "timer_${name}_complete",
            mapOf(
                "ticks" to ticks,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_complete
         *
         * The timer named (name) has completed.
         *
         * Note that this timer may reset and start again after this event is
         * posted, depending on its settings.
         *
         * Args:
         *   ticks: The current tick number this timer is at
         *   ticks_remaining: The number of ticks in this timer remaining
         */

        if (restartOnComplete == true) {
            debugLog("Restart on complete: True")
            restart()
        }
    }

    /**
     * Timer tick - automatically called by the core clock each tick.
     */
    private fun timerTick() {
        debugLog("Timer Tick")

        if (!running) {
            debugLog("Timer is not running. Will remove.")
            removeSystemTimer()
            return
        }

        if (direction == "down") {
            ticks -= 1
        } else {
            ticks += 1
        }

        postTickEvents()
    }

    /**
     * Post tick events.
     */
    private fun postTickEvents() {
        if (!checkForDone()) {
            machine.events.post(
                "timer_${name}_tick",
                mapOf(
                    "ticks" to ticks,
                    "ticks_remaining" to ticksRemaining
                )
            )
            /**
             * Event: timer_(name)_tick
             *
             * The timer named (name) has just counted down (or up,
             * depending on its settings).
             *
             * Args:
             *   ticks: The new tick number this timer is at
             *   ticks_remaining: The new number of ticks in this timer remaining
             */

            debugLog("Ticks: $ticks, Remaining: $ticksRemaining")
        }
    }

    /**
     * Add ticks to this timer.
     *
     * @param timerValue The number of ticks to add
     */
    fun add(timerValue: Int) {
        val ticksAdded = timerValue

        var newValue = ticks + ticksAdded

        if (maxValue != null && newValue > maxValue!!) {
            newValue = maxValue!!
        }

        ticks = newValue

        machine.events.post(
            "timer_${name}_time_added",
            mapOf(
                "ticks" to ticks,
                "ticks_added" to ticksAdded,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_time_added
         *
         * The timer named (name) has just had time added to it.
         *
         * Args:
         *   ticks: The new tick number this timer is at
         *   ticks_remaining: The new number of ticks in this timer remaining
         *   ticks_added: How many ticks were just added
         */

        checkForDone()
    }

    /**
     * Subtract ticks from this timer.
     *
     * @param timerValue The number of ticks to subtract
     */
    fun subtract(timerValue: Int) {
        val ticksSubtracted = timerValue

        ticks -= ticksSubtracted

        machine.events.post(
            "timer_${name}_time_subtracted",
            mapOf(
                "ticks" to ticks,
                "ticks_subtracted" to ticksSubtracted,
                "ticks_remaining" to ticksRemaining
            )
        )
        /**
         * Event: timer_(name)_time_subtracted
         *
         * The timer named (name) just had some ticks removed.
         *
         * Args:
         *   ticks: The new current tick number this timer is at
         *   ticks_remaining: The new number of ticks in this timer remaining
         *   ticks_subtracted: How many ticks were just subtracted
         */

        checkForDone()
    }

    /**
     * Check if timer is done.
     *
     * @return true if timer is complete
     */
    private fun checkForDone(): Boolean {
        debugLog("Checking to see if timer is done. Ticks: $ticks, End Value: $endValue, Direction: $direction")

        if (direction == "up" && endValue != null && ticks >= endValue!!) {
            timerComplete()
            return true
        }

        if (direction == "down" && endValue != null && ticks <= endValue!!) {
            timerComplete()
            return true
        }

        if (endValue != null) {
            ticksRemaining = kotlin.math.abs(endValue!! - ticks)
        }

        debugLog("Timer is not done")

        return false
    }

    /**
     * Create the clock event which drives this mode timer's tick method.
     */
    private fun createSystemTimer() {
        removeSystemTimer()
        // TODO: Create timer when clock is available
        /*
        timer = machine.clock.scheduleInterval(::timerTick, tickSecs ?: 1.0)
        */
    }

    /**
     * Remove the clock event associated with this mode timer.
     */
    private fun removeSystemTimer() {
        timer?.let {
            // TODO: Unschedule when clock is available
            /*
            machine.clock.unschedule(it)
            */
            timer = null
        }
    }

    /**
     * Change the interval for each tick of this timer.
     *
     * @param change Multiplier for the current tick interval
     */
    fun changeTickInterval(change: Double) {
        tickSecs = (tickSecs ?: 1.0) * change
        createSystemTimer()
    }

    /**
     * Set the number of seconds between ticks for this timer.
     *
     * @param timerValue The new number of seconds between each tick
     */
    fun setTickInterval(timerValue: Double) {
        tickSecs = kotlin.math.abs(timerValue)
        createSystemTimer()
    }

    /**
     * Set the current amount of time of this timer.
     *
     * @param timerValue Integer of the current value
     */
    fun jump(timerValue: Int) {
        ticks = timerValue

        if (maxValue != null && ticks > maxValue!!) {
            ticks = maxValue!!
        }

        removeSystemTimer()
        createSystemTimer()

        checkForDone()
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        stop()
        removeControlEvents()
    }
}
