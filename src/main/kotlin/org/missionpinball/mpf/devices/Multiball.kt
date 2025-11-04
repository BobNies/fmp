package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Multiball device for MPF.
 *
 * Features:
 * - Configurable ball count (total or add)
 * - Shoot again with timer
 * - Grace period and hurry up states
 * - Add-a-ball support with separate ball save
 * - Ball lock integration for ball sources
 * - Replace balls in play option
 *
 * Note: In Python, this uses @DeviceMonitor("shoot_again", "grace_period", "hurry_up", "balls_added_live", "balls_live_target") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this extends EnableDisableMixin.
 * In Kotlin, we implement enable/disable functionality directly.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class Multiball(machine: MachineController, name: String) :
    SystemWideDevice(machine, name), ModeDevice(machine, name) {

    override val configSection = "multiballs"
    override val collection = "multiballs"
    override val classLabel = "multiball"

    /**
     * Ball locks that can provide balls.
     */
    private var ballLocks: List<BallDevice>? = null

    /**
     * Source playfield for ball ejections.
     */
    private var sourcePlayfield: Any? = null

    /**
     * Delay manager for timing operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Number of balls added and currently live.
     */
    var ballsAddedLive = 0

    /**
     * Target number of balls that should be live.
     */
    var ballsLiveTarget = 0

    /**
     * Whether shoot again is active.
     */
    var shootAgain = false

    /**
     * Whether grace period is active.
     */
    var gracePeriod = false

    /**
     * Whether hurry up is active.
     */
    var hurryUp = false

    /**
     * Whether the multiball is enabled.
     */
    private var _enabled = false

    override val canExistOutsideOfGame: Boolean
        get() = true

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)

        // Also stop mb if shoot again is specified (aka the MB is currently running)
        if (shootAgain) {
            stop()
        }
    }

    override suspend fun initialize() {
        super.initialize()
        // TODO: Get ball locks and source playfield when available
        /*
        ballLocks = config["ball_locks"] as? List<BallDevice>
        sourcePlayfield = config["source_playfield"]
        */

        // TODO: Evaluate placeholder when available
        /*
        val ballCount = (config["ball_count"] as? NativeTypeTemplate)?.evaluate(emptyList())
        */
        val ballCount = config["ball_count"] as? Int

        val ballCountType = config["ball_count_type"] as? String ?: "add"
        if (ballCountType == "total" && ballCount != null && ballCount <= 1) {
            raiseConfigError("ball_count should be at least 2 for a multiball to have an effect when " +
                "ball_count_type is set to total.", 1)
        } else if (ballCountType == "add" && ballCount != null && ballCount <= 0) {
            raiseConfigError("ball_count should be at least 1 for a multiball to have an effect when " +
                "ball_count_type is set to add.", 2)
        }
    }

    companion object {
        /**
         * Add default events when outside mode.
         */
        @JvmStatic
        fun prepareConfig(config: MutableMap<String, Any?>, isModeConfig: Boolean): Map<String, Any?> {
            if (!isModeConfig) {
                if ("enable_events" !in config) {
                    config["enable_events"] = "ball_started"
                }
                if ("disable_events" !in config) {
                    config["disable_events"] = "ball_will_end"
                }
            }
            return config
        }
    }

    /**
     * Handle balls in play and balls live calculation.
     */
    private fun handleBallsInPlayAndBallsLive() {
        // TODO: Evaluate placeholder when available
        /*
        val ballCount = (config["ball_count"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 2
        */
        val ballCount = (config["ball_count"] as? Int) ?: 2
        val game = machine.game ?: return

        val replaceBallsInPlay = config["replace_balls_in_play"] as? Boolean ?: false
        val ballsToReplace = if (replaceBallsInPlay) game.ballsInPlay else 0
        debugLog("Going to add an additional $ballsToReplace balls for replace_balls_in_play")

        val ballCountType = config["ball_count_type"] as? String ?: "add"
        if (ballCountType == "total") {
            // Policy: total balls
            if (ballCount > game.ballsInPlay) {
                ballsAddedLive = ballCount - game.ballsInPlay
                game.ballsInPlay = ballCount
            }
            ballsLiveTarget = ballCount
        } else {
            // Policy: add balls
            ballsAddedLive = ballCount
            game.ballsInPlay += ballsAddedLive
            ballsLiveTarget = game.ballsInPlay
        }

        ballsAddedLive += ballsToReplace
    }

    /**
     * Event handler for start event.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     */
    fun eventStart() {
        start()
    }

    /**
     * Start multiball.
     */
    fun start() {
        if (!_enabled) {
            return
        }

        if (ballsLiveTarget > 0) {
            debugLog("Cannot start MB because $ballsLiveTarget are still in play")
            return
        }

        shootAgain = true

        handleBallsInPlayAndBallsLive()
        debugLog("Starting multiball with $ballsLiveTarget balls (added $ballsAddedLive)")

        var ballsAdded = 0

        // TODO: Eject balls from locks when BallDevice is available
        /*
        // Eject balls from locks
        for (device in ballLocks ?: emptyList()) {
            val ballsToRelease = maxOf(minOf(device.availableBalls, ballsAddedLive - ballsAdded), 0)
            sourcePlayfield.addBall(balls = ballsToRelease, sourceDevice = device)
            ballsAdded += ballsToRelease
        }

        // Request remaining balls
        if (ballsAddedLive - ballsAdded > 0) {
            sourcePlayfield.addBall(balls = ballsAddedLive - ballsAdded)
        }
        */

        // TODO: Evaluate placeholder when available
        /*
        val shootAgainMs = (config["shoot_again"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        */
        val shootAgainMs = (config["shoot_again"] as? Int) ?: 0
        if (shootAgainMs == 0) {
            // No shoot again. Just stop multiball right away
            stop()
        } else {
            // Enable shoot again
            // TODO: Register event handler when available
            /*
            machine.events.addHandler("ball_drain", ::ballDrainShootAgain, priority = 1000)
            */
            timerStart()
        }

        machine.events.post(
            "multiball_${name}_started",
            mapOf("balls" to ballsLiveTarget)
        )
        /**
         * Event: multiball_(name)_started
         *
         * The multiball called (name) has just started.
         *
         * Args:
         *   balls: The number of balls in this multiball
         */
    }

    /**
     * Start the timer.
     *
     * This is started when multiball starts if configured.
     */
    private fun timerStart() {
        machine.events.post("ball_save_${name}_timer_start")
        /**
         * Event: ball_save_(name)_timer_start
         *
         * The multiball ball save called (name) has just started its countdown timer.
         */

        // TODO: Evaluate placeholders when available
        /*
        val shootAgainMs = (config["shoot_again"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        val gracePeriodMs = (config["grace_period"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        val hurryUpTimeMs = (config["hurry_up_time"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        */
        val shootAgainMs = (config["shoot_again"] as? Int) ?: 0
        val gracePeriodMs = (config["grace_period"] as? Int) ?: 0
        val hurryUpTimeMs = (config["hurry_up_time"] as? Int) ?: 0

        startShootAgain(shootAgainMs.toLong(), gracePeriodMs.toLong(), hurryUpTimeMs.toLong())
    }

    /**
     * Set callbacks for shoot again, grace period, and hurry up, if values above 0 are provided.
     *
     * This is started for both beginning multiball ball save and add a ball ball save.
     *
     * @param shootAgainMs Shoot again duration in milliseconds
     * @param gracePeriodMs Grace period duration in milliseconds
     * @param hurryUpTimeMs Time before shoot again ends to start hurry up, in milliseconds
     */
    private fun startShootAgain(shootAgainMs: Long, gracePeriodMs: Long, hurryUpTimeMs: Long) {
        if (shootAgainMs > 0) {
            debugLog("Starting ball save timer: ${shootAgainMs}ms")
            delay.add(
                name = "disable_shoot_again",
                ms = shootAgainMs + gracePeriodMs,
                callback = { stop() }
            )
        }
        if (gracePeriodMs > 0) {
            gracePeriod = true
            delay.add(
                name = "grace_period",
                ms = shootAgainMs,
                callback = { gracePeriodCallback() }
            )
        }
        if (hurryUpTimeMs > 0) {
            hurryUp = true
            delay.add(
                name = "hurry_up",
                ms = shootAgainMs - hurryUpTimeMs,
                callback = { hurryUpCallback() }
            )
        }
    }

    /**
     * Start hurry up mode.
     */
    private fun hurryUpCallback() {
        debugLog("Starting Hurry Up")

        hurryUp = false
        machine.events.post("multiball_${name}_hurry_up")
        /**
         * Event: multiball_(name)_hurry_up
         *
         * The multiball ball save called (name) has just entered its hurry up mode.
         */
    }

    /**
     * Start grace period.
     */
    private fun gracePeriodCallback() {
        debugLog("Starting Grace Period")

        gracePeriod = false
        machine.events.post("multiball_${name}_grace_period")
        /**
         * Event: multiball_(name)_grace_period
         *
         * The multiball ball save called (name) has just entered its grace period time.
         */
    }

    /**
     * Handle ball drain during shoot again.
     *
     * @param balls Number of balls that drained
     * @return Map with unclaimed balls
     */
    private fun ballDrainShootAgain(balls: Int): Map<String, Int> {
        val game = machine.game ?: return mapOf("balls" to balls)

        val ballsToSave = ballsLiveTarget - game.ballsInPlay + balls

        if (ballsToSave <= 0) {
            return mapOf("balls" to balls)
        }

        val actualBallsToSave = if (ballsToSave > balls) balls else ballsToSave

        machine.events.post(
            "multiball_${name}_shoot_again",
            mapOf("balls" to actualBallsToSave)
        )
        /**
         * Event: multiball_(name)_shoot_again
         *
         * A ball has drained during the multiball called (name) while the ball save
         * timer for that multiball was running, so a ball (or balls) will be saved
         * and re-added into play.
         *
         * Args:
         *   balls: The number of balls that are being saved
         */

        debugLog("Ball drained during MB. Requesting a new one")
        // TODO: Add ball when playfield is available
        /*
        sourcePlayfield.addBall(balls = actualBallsToSave)
        */
        return mapOf("balls" to (balls - actualBallsToSave))
    }

    /**
     * Count balls after shoot again expires.
     *
     * @param balls Number of balls that drained
     */
    private fun ballDrainCountBalls(balls: Int) {
        machine.events.post("multiball_${name}_ball_lost")
        /**
         * Event: multiball_(name)_lost_ball
         *
         * The multiball called (name) has lost a ball after ball save expired.
         */

        val game = machine.game
        if (game == null || game.ballsInPlay - balls < 1) {
            ballsAddedLive = 0
            ballsLiveTarget = 0
            // TODO: Remove event handler when available
            /*
            machine.events.removeHandler(::ballDrainCountBalls)
            */
            machine.events.post("multiball_${name}_ended")
            /**
             * Event: multiball_(name)_ended
             *
             * The multiball called (name) has just ended.
             */
            debugLog("Ball drained. MB ended.")
        }
    }

    /**
     * Event handler for stop event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventStop() {
        stop()
    }

    /**
     * Stop shoot again.
     */
    fun stop() {
        debugLog("Stopping shoot again of multiball")
        shootAgain = false

        // Disable shoot again
        // TODO: Remove event handler when available
        /*
        machine.events.removeHandler(::ballDrainShootAgain)
        */

        if (gracePeriod) {
            // TODO: Remove event handler when available
            /*
            machine.events.removeHandler(::gracePeriodCallback)
            */
            gracePeriodCallback()
        }
        if (hurryUp) {
            // TODO: Remove event handler when available
            /*
            machine.events.removeHandler(::hurryUpCallback)
            */
            hurryUpCallback()
        }
        machine.events.post("multiball_${name}_shoot_again_ended")
        /**
         * Event: multiball_(name)_shoot_again_ended
         *
         * Shoot again for multiball (name) has ended.
         */

        // Add handler for ball_drain until self.balls_ejected are drained
        // TODO: Remove and add event handlers when available
        /*
        machine.events.removeHandler(::ballDrainCountBalls)
        machine.events.addHandler("ball_drain", ::ballDrainCountBalls)
        */
    }

    /**
     * Event handler for add_a_ball event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventAddABall() {
        addABall()
    }

    /**
     * Add a ball if multiball has started.
     */
    fun addABall() {
        if (ballsLiveTarget > 0) {
            debugLog("Adding a ball.")
            ballsLiveTarget += 1
            ballsAddedLive += 1
            machine.game?.let { it.ballsInPlay += 1 }
            // TODO: Add ball when playfield is available
            /*
            sourcePlayfield.addBall(balls = 1)
            */
            addABallTimerStart()
        }
    }

    /**
     * Start the timer for add a ball ball save.
     *
     * This is started when multiball add a ball is triggered if configured,
     * and the default timer is not still running.
     */
    private fun addABallTimerStart() {
        if (shootAgain) {
            // If main ball save timer is running, don't run this timer
            return
        }
        shootAgain = true

        // TODO: Evaluate placeholder when available
        /*
        val shootAgainMs = (config["add_a_ball_shoot_again"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        */
        val shootAgainMs = (config["add_a_ball_shoot_again"] as? Int) ?: 0
        if (shootAgainMs == 0) {
            // No shoot again. Just stop multiball right away
            stop()
            return
        }
        // Enable shoot again
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("ball_drain", ::ballDrainShootAgain, priority = 1000)
        */

        machine.events.post("ball_save_${name}_add_a_ball_timer_start")
        /**
         * Event: ball_save_(name)_add_a_ball_timer_start
         *
         * The multiball add a ball ball save called (name) has just started its countdown timer.
         */

        // TODO: Evaluate placeholders when available
        /*
        val gracePeriodMs = (config["add_a_ball_grace_period"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        val hurryUpTimeMs = (config["add_a_ball_hurry_up_time"] as? NativeTypeTemplate)?.evaluate(emptyList()) ?: 0
        */
        val gracePeriodMs = (config["add_a_ball_grace_period"] as? Int) ?: 0
        val hurryUpTimeMs = (config["add_a_ball_hurry_up_time"] as? Int) ?: 0
        startShootAgain(shootAgainMs.toLong(), gracePeriodMs.toLong(), hurryUpTimeMs.toLong())
    }

    /**
     * Event handler for start_or_add_a_ball event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventStartOrAddABall() {
        startOrAddABall()
    }

    /**
     * Start multiball or add a ball if multiball has started.
     */
    fun startOrAddABall() {
        if (ballsLiveTarget > 0) {
            addABall()
        } else {
            start()
        }
    }

    /**
     * Event handler for reset event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventReset() {
        reset()
    }

    /**
     * Reset the multiball and disable it.
     */
    fun reset() {
        disable()
        shootAgain = false
        ballsAddedLive = 0
    }

    /**
     * Enable the multiball.
     */
    fun enable() {
        _enabled = true
    }

    /**
     * Disable the multiball.
     */
    fun disable() {
        _enabled = false
    }
}
