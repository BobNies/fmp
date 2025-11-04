package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Ball save device which will give back the ball within a certain time.
 *
 * Features:
 * - Timer-based ball save with hurry up and grace period
 * - Early ball save support
 * - Delayed eject support
 * - Configurable number of saves
 * - Unlimited saves mode
 * - Integration with ball locks for ball replacement
 *
 * Note: In Python, this uses @DeviceMonitor("saves_remaining", "enabled", "timer_started", "state") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class BallSave(machine: MachineController, name: String) :
    SystemWideDevice(machine, name), ModeDevice(machine, name) {

    override val configSection = "ball_saves"
    override val collection = "ball_saves"
    override val classLabel = "ball_save"

    /**
     * Ball locks that can provide balls for replacement.
     */
    private var ballLocks: List<BallDevice>? = null

    /**
     * Active time in seconds.
     */
    var activeTime: Int = 0

    /**
     * Whether unlimited saves are enabled.
     */
    private var unlimitedSaves: Boolean = false

    /**
     * Source playfield for ball ejections.
     */
    private var sourcePlayfield: Any? = null

    /**
     * Delay manager for timing operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Whether the ball save is enabled.
     */
    var enabled = false

    /**
     * Whether the timer has started.
     */
    var timerStarted = false

    /**
     * Number of saves remaining.
     */
    var savesRemaining = 0

    /**
     * Number of early saves performed (waiting for drain).
     */
    private var earlySaved = 0

    /**
     * Current state (disabled, enabled, hurry_up, grace_period).
     */
    var state = "disabled"

    /**
     * Number of balls scheduled but not yet ejected.
     */
    private var scheduledBalls = 0

    override suspend fun initialize() {
        super.initialize()
        // TODO: Get ball locks and source playfield when available
        /*
        ballLocks = config["ball_locks"] as? List<BallDevice>
        unlimitedSaves = (config["balls_to_save"] as? Int) == -1
        sourcePlayfield = config["source_playfield"]
        */
    }

    override val canExistOutsideOfGame: Boolean
        get() = true

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix).toMutableMap()

        // Make sure timer_start_events are not in enable_events
        val timerStartEvents = validatedConfig["timer_start_events"] as? List<*> ?: emptyList<String>()
        val enableEvents = validatedConfig["enable_events"] as? List<*> ?: emptyList<String>()

        for (event in timerStartEvents) {
            if (event in enableEvents) {
                throw AssertionError("$event in timer_start_events will not work because it is also in " +
                    "enable_events. Omit it!")
            }
        }

        if (validatedConfig["delayed_eject_events"] != null && validatedConfig["eject_delay"] != null) {
            throw AssertionError("cannot use delayed_eject_events and eject_delay at the same time.")
        }

        return validatedConfig
    }

    override fun enable() {
        super.enable()
        if (enabled) {
            return
        }

        savesRemaining = (config["balls_to_save"] as? Int) ?: 1
        earlySaved = 0
        enabled = true
        state = "enabled"
        // TODO: Evaluate placeholder when available
        /*
        activeTime = (config["active_time"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        */
        activeTime = (config["active_time"] as? Int) ?: 0

        debugLog("Enabling. Auto launch: ${config["auto_launch"]}, Balls to save: ${config["balls_to_save"]}, Active time: ${activeTime}s")

        // Enable shoot again
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("ball_drain", ::ballDrainWhileActive, priority = 1000)
        */

        val timerStartEvents = config["timer_start_events"] as? List<*>
        if (activeTime > 0 && (timerStartEvents == null || timerStartEvents.isEmpty())) {
            timerStart()
        }

        machine.events.post("ball_save_${name}_enabled")
        /**
         * Event: ball_save_(name)_enabled
         *
         * The ball save called (name) has just been enabled.
         */
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    override fun disable() {
        if (!enabled) {
            return
        }

        enabled = false
        state = "disabled"
        timerStarted = false
        debugLog("Disabling...")
        // TODO: Remove event handler when available
        /*
        machine.events.removeHandler(::ballDrainWhileActive)
        */
        delay.remove("disable")
        delay.remove("hurry_up")
        delay.remove("grace_period")

        machine.events.post("ball_save_${name}_disabled")
        /**
         * Event: ball_save_(name)_disabled
         *
         * The ball save called (name) has just been disabled.
         */
    }

    /**
     * Event handler for timer start event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventTimerStart() {
        timerStart()
    }

    /**
     * Start the timer.
     *
     * This is usually called after the ball was ejected while the ball save may
     * have been enabled earlier.
     */
    fun timerStart() {
        if (timerStarted || !enabled) {
            return
        }

        timerStarted = true

        machine.events.post("ball_save_${name}_timer_start")
        /**
         * Event: ball_save_(name)_timer_start
         *
         * The ball save called (name) has just started its countdown timer.
         */

        if (activeTime > 0) {
            debugLog("Starting ball save timer: ${activeTime}s")
            val activeTimeMs = activeTime * 1000L
            val gracePeriod = (config["grace_period"] as? Int) ?: 0
            val hurryUpTime = (config["hurry_up_time"] as? Int) ?: 0

            delay.add(
                name = "disable",
                ms = activeTimeMs + gracePeriod,
                callback = { disable() }
            )
            delay.add(
                name = "grace_period",
                ms = activeTimeMs,
                callback = { gracePeriod() }
            )
            delay.add(
                name = "hurry_up",
                ms = activeTimeMs - hurryUpTime,
                callback = { hurryUp() }
            )
        }
    }

    /**
     * Start hurry up mode.
     */
    private fun hurryUp() {
        debugLog("Starting Hurry Up")

        state = "hurry_up"

        machine.events.post("ball_save_${name}_hurry_up")
        /**
         * Event: ball_save_(name)_hurry_up
         *
         * The ball save called (name) has just entered its hurry up mode.
         */
    }

    /**
     * Start grace period.
     */
    private fun gracePeriod() {
        debugLog("Starting Grace Period")

        state = "grace_period"

        machine.events.post("ball_save_${name}_grace_period")
        /**
         * Event: ball_save_(name)_grace_period
         *
         * The ball save called (name) has just entered its grace period time.
         */
    }

    /**
     * Get the number of balls to save based on current conditions.
     *
     * @param availableBalls Number of balls available to save
     * @return Number of balls to actually save
     */
    private fun getNumberOfBallsToSave(availableBalls: Int): Int {
        val game = machine.game
        if (game == null || game.ballsInPlay <= 0) {
            debugLog("Received request to save ball, but no balls are in play. Discarding request.")
            return 0
        }

        if (game.ballsInPlay > 0) {
            val onlyLastBall = config["only_last_ball"] as? Boolean ?: false
            if (onlyLastBall && game.ballsInPlay > 1) {
                debugLog("Will only save last ball but ${game.ballsInPlay} are in play.")
                return 0
            }
        }

        var ballsToSave = availableBalls

        val onlyLastBall = config["only_last_ball"] as? Boolean ?: false
        if (onlyLastBall && ballsToSave > 1) {
            ballsToSave = 1
        }

        if (ballsToSave > game.ballsInPlay) {
            ballsToSave = game.ballsInPlay
        }

        if (ballsToSave > savesRemaining && !unlimitedSaves) {
            ballsToSave = savesRemaining
        }

        return ballsToSave
    }

    /**
     * Reduce remaining saves and disable if zero.
     *
     * @param ballsToSave Number of balls being saved
     */
    private fun reduceRemainingSavesAndDisableIfZero(ballsToSave: Int) {
        if (!unlimitedSaves) {
            savesRemaining -= ballsToSave
            debugLog("Saves remaining: $savesRemaining")
        } else {
            debugLog("Unlimited saves remaining")
        }

        if (savesRemaining <= 0 && !unlimitedSaves) {
            debugLog("Disabling since there are no saves remaining")
            disable()
        }
    }

    /**
     * Handle ball drain while ball save is active.
     *
     * @param balls Number of balls that drained
     * @return Map with unclaimed balls
     */
    private fun ballDrainWhileActive(balls: Int): Map<String, Int> {
        if (balls <= 0) {
            return emptyMap()
        }

        val ballsToSave = getNumberOfBallsToSave(balls)

        debugLog("Ball(s) drained while active. Requesting new one(s). Auto launch: ${config["auto_launch"]}")

        machine.events.post(
            "ball_save_${name}_saving_ball",
            mapOf(
                "balls" to ballsToSave,
                "early_save" to false
            )
        )
        /**
         * Event: ball_save_(name)_saving_ball
         *
         * The ball save called (name) has just saved one (or more) balls.
         *
         * Args:
         *   balls: The number of balls this ball saver is saving
         *   early_save: True if this is an early ball save
         */

        scheduleBalls(ballsToSave)

        reduceRemainingSavesAndDisableIfZero(ballsToSave)

        return mapOf("balls" to (balls - ballsToSave))
    }

    /**
     * Event handler for early_ball_save event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventEarlyBallSave() {
        earlyBallSave()
    }

    /**
     * Perform early ball save if enabled.
     */
    fun earlyBallSave() {
        if (!enabled) {
            return
        }

        if (getNumberOfBallsToSave(1) == 0) {
            return
        }

        if (earlySaved > 0) {
            debugLog("Already performed an early ball save. Ball needs to drain first.")
            return
        }

        machine.events.post(
            "ball_save_${name}_saving_ball",
            mapOf(
                "balls" to 1,
                "early_save" to true
            )
        )
        // Event documented above

        debugLog("Performing early ball save.")
        earlySaved += 1
        scheduleBalls(1)
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("ball_drain", ::earlyBallSaveDrainHandler, priority = 1001)
        */

        reduceRemainingSavesAndDisableIfZero(1)
    }

    /**
     * Handle ball drain after early ball save.
     *
     * @param balls Number of balls that drained
     * @return Map with unclaimed balls
     */
    private fun earlyBallSaveDrainHandler(balls: Int): Map<String, Int> {
        if (earlySaved > 0 && balls > 0) {
            earlySaved -= 1
            debugLog("Early saved ball drained.")
            // TODO: Remove event handler when available
            /*
            machine.events.removeHandler(::earlyBallSaveDrainHandler)
            */
            return mapOf("balls" to (balls - 1))
        }

        return emptyMap()
    }

    /**
     * Schedule balls for ejection.
     *
     * @param ballsToSave Number of balls to schedule
     */
    private fun scheduleBalls(ballsToSave: Int) {
        val ejectDelay = config["eject_delay"] as? Int
        val delayedEjectEvents = config["delayed_eject_events"] as? List<*>

        when {
            ejectDelay != null && ejectDelay > 0 -> {
                // Schedule after delay to add some drama
                delay.add(ejectDelay.toLong(), "eject") {
                    addBalls(ballsToSave)
                }
            }
            delayedEjectEvents != null && delayedEjectEvents.isNotEmpty() -> {
                // Unlimited delay. Wait for event
                scheduledBalls += ballsToSave
            }
            else -> {
                // Default: no delay. Just eject balls right now
                addBalls(ballsToSave)
            }
        }
    }

    /**
     * Event handler for delayed_eject event.
     *
     * TODO: Add @EventHandler(4) annotation when event system is fully integrated
     */
    fun eventDelayedEject() {
        delayedEject()
    }

    /**
     * Trigger eject of all scheduled balls.
     */
    fun delayedEject() {
        addBalls(scheduledBalls)
        scheduledBalls = 0
    }

    /**
     * Add balls to the playfield.
     *
     * @param ballsToSave Number of balls to add
     */
    private fun addBalls(ballsToSave: Int) {
        var ballsAdded = 0

        // TODO: Eject balls from locks when BallDevice is available
        /*
        // Eject balls from locks
        for (device in ballLocks ?: emptyList()) {
            val ballsToRelease = maxOf(minOf(device.availableBalls, ballsToSave - ballsAdded), 0)
            val autoLaunch = config["auto_launch"] as? Boolean ?: false
            sourcePlayfield.addBall(
                balls = ballsToRelease,
                sourceDevice = device,
                playerControlled = !autoLaunch
            )
            ballsAdded += ballsToRelease
        }

        // Request remaining balls
        if (ballsToSave - ballsAdded > 0) {
            val autoLaunch = config["auto_launch"] as? Boolean ?: false
            sourcePlayfield.addBall(
                balls = ballsToSave - ballsAdded,
                playerControlled = !autoLaunch
            )
        }
        */
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        debugLog("Removing...")

        disable()

        val delayedEjectEvents = config["delayed_eject_events"] as? List<*>
        if (delayedEjectEvents != null && delayedEjectEvents.isNotEmpty()) {
            debugLog("Triggering delayed eject because mode ended.")
            delayedEject()
        }
    }
}
