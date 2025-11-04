package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice
import java.util.*

/**
 * Ball hold device which can be used to keep balls in ball devices and control their eject later on.
 *
 * Ball holds enable/disable to control when they capture balls, track how many balls are held,
 * and can release them individually or all at once.
 *
 * Note: In Python, this uses @DeviceMonitor("balls_held") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this extends EnableDisableMixin.
 * In Kotlin, we implement enable/disable functionality directly.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class BallHold(machine: MachineController, name: String) :
    SystemWideDevice(machine, name), ModeDevice(machine, name) {

    override val configSection = "ball_holds"
    override val collection = "ball_holds"
    override val classLabel = "ball_hold"

    /**
     * List of ball devices that this hold uses.
     */
    var holdDevices: List<BallDevice>? = null

    /**
     * Source playfield.
     */
    var sourcePlayfield: Any? = null

    /**
     * Number of balls currently held.
     */
    var ballsHeld = 0

    /**
     * Number of balls released but not yet drained.
     */
    private var releasedBalls = 0

    /**
     * Queue for release hold.
     */
    private var releaseHold: Any? = null

    /**
     * Queue tracking which devices hold which balls.
     */
    private val holdQueue = ArrayDeque<Pair<BallDevice, Int>>()

    /**
     * Whether the hold is enabled.
     */
    private var _enabled = false

    init {
        // TODO: Register event handler when available
        /*
        machine.events.addHandler("init_phase_3", ::initializeLate)
        */
    }

    override val canExistOutsideOfGame: Boolean
        get() = true

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

    override suspend fun initialize() {
        super.initialize()

        val devices = config["hold_devices"] as? List<*> ?: emptyList<BallDevice>()
        holdDevices = devices.mapNotNull { it as? BallDevice }

        sourcePlayfield = config["source_playfield"]
    }

    /**
     * Late initialization after ball devices are ready.
     */
    private fun initializeLate() {
        // Postpone until ball devices are ready
        val ballsToHold = config["balls_to_hold"] as? Int
        if (ballsToHold == null || ballsToHold == 0) {
            var totalCapacity = 0
            for (device in holdDevices ?: emptyList()) {
                totalCapacity += device.capacity
            }
            config["balls_to_hold"] = totalCapacity
        }
    }

    /**
     * Enable the hold.
     *
     * If the hold is not enabled, no balls will be held.
     */
    private fun enable() {
        debugLog("Enabling...")
        _enabled = true
        registerHandlers()
    }

    /**
     * Disable the hold.
     *
     * If the hold is not enabled, no balls will be held.
     */
    private fun disable() {
        debugLog("Disabling...")
        _enabled = false
        unregisterHandlers()
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
     * Reset the hold.
     *
     * Will release held balls. Device status will stay the same
     * (enabled/disabled). It will wait for those balls to drain and block
     * ball_ending until they do. Those balls are not included in ball_in_play.
     */
    fun reset() {
        releasedBalls += releaseAll()
        ballsHeld = 0

        if (releasedBalls > 0) {
            // Add handler for ball_drain until releasedBalls are drained
            // TODO: Register event handler when available
            /*
            machine.events.addHandler("ball_drain", ::waitForDrain)
            */

            // Block ball_ending
            // TODO: Register event handler when available
            /*
            machine.events.addHandler("ball_ending", ::blockDuringDrain, priority = 10000)
            */
        }
    }

    /**
     * Wait for drained balls to be processed.
     */
    private fun waitForDrain(balls: Int): Map<String, Int> {
        if (balls <= 0) {
            return mapOf("balls" to balls)
        }

        val ballToReduce = if (balls > releasedBalls) releasedBalls else balls

        releasedBalls -= ballToReduce
        debugLog("$ballToReduce ball of hold drained.")

        if (releasedBalls <= 0) {
            releaseHold?.let {
                // TODO: Clear queue when available
                /*
                it.clear()
                */
                releaseHold = null
            }
            debugLog("All released balls of ball_hold drained.")
            // TODO: Remove event handlers when available
            /*
            machine.events.removeHandlerByEvent("ball_ending", ::waitForDrain)
            machine.events.removeHandlerByEvent("ball_drain", ::blockDuringDrain)
            */
        }

        return mapOf("balls" to (balls - ballToReduce))
    }

    /**
     * Block during drain.
     */
    private fun blockDuringDrain(queue: Any) {
        if (releasedBalls > 0) {
            // TODO: Wait on queue when available
            /*
            queue.wait()
            */
            releaseHold = queue
        }
    }

    /**
     * Event handler for release_one_if_full event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventReleaseOneIfFull() {
        releaseOneIfFull()
    }

    /**
     * Release one ball if hold is full.
     */
    fun releaseOneIfFull() {
        if (isFull()) {
            releaseOne()
        }
    }

    /**
     * Event handler for release_one event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventReleaseOne() {
        releaseOne()
    }

    /**
     * Release one ball.
     */
    fun releaseOne() {
        releaseBalls(ballsToRelease = 1)
    }

    /**
     * Event handler for release_all event.
     *
     * TODO: Add @EventHandler(7) annotation when event system is fully integrated
     */
    fun eventReleaseAll() {
        releaseAll()
    }

    /**
     * Release all balls in hold.
     */
    fun releaseAll(): Int {
        return releaseBalls(ballsHeld)
    }

    /**
     * Release balls and return the actual amount of balls released.
     *
     * @param ballsToRelease Number of balls to release from hold
     * @return Number of balls actually released
     */
    fun releaseBalls(ballsToRelease: Int): Int {
        if (holdQueue.isEmpty()) {
            return 0
        }

        var remainingBallsToRelease = ballsToRelease

        debugLog("Releasing up to $ballsToRelease balls from hold")
        var ballsReleased = 0

        while (holdQueue.isNotEmpty()) {
            val (device, ballsHeldInDevice) = holdQueue.removeLast()
            var balls = ballsHeldInDevice
            val ballsInDevice = device.balls

            if (balls > ballsInDevice) {
                balls = ballsInDevice
            }

            if (balls > remainingBallsToRelease) {
                holdQueue.addLast(Pair(device, ballsHeldInDevice - remainingBallsToRelease))
                balls = remainingBallsToRelease
            }

            // TODO: Eject when ball device is available
            /*
            device.eject(balls = balls)
            */
            ballsReleased += balls
            remainingBallsToRelease -= balls

            if (remainingBallsToRelease <= 0) {
                break
            }
        }

        if (ballsReleased > 0) {
            machine.events.post(
                "ball_hold_${name}_balls_released",
                mapOf("balls_released" to ballsReleased)
            )
            /**
             * Event: ball_hold_(name)_balls_released
             *
             * The ball hold device (name) has just released ball(s).
             *
             * Args:
             *   balls_released: The number of balls that were just released
             */
        }

        ballsHeld -= ballsReleased
        return ballsReleased
    }

    /**
     * Register event handlers.
     */
    private fun registerHandlers() {
        val priority = (mode?.priority ?: 0) + (config["priority"] as? Int ?: 0)

        // Register on ball_enter of hold_devices
        for (device in holdDevices ?: emptyList()) {
            // TODO: Register event handlers when available
            /*
            machine.events.addHandler(
                "balldevice_${device.name}_ball_enter",
                { args -> holdBall(device, args) },
                priority = priority
            )
            machine.events.addHandler(
                "balldevice_${device.name}_ball_missing",
                { args -> lostBall(device, args) },
                priority = priority
            )
            */
        }
    }

    /**
     * Unregister event handlers.
     */
    private fun unregisterHandlers() {
        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandler(::holdBall)
        machine.events.removeHandler(::lostBall)
        */
    }

    /**
     * Return true if hold is full.
     */
    fun isFull(): Boolean {
        return remainingSpaceInHold() == 0
    }

    /**
     * Return the remaining capacity of the hold.
     */
    fun remainingSpaceInHold(): Int {
        val ballsToHold = config["balls_to_hold"] as? Int ?: 0
        val remaining = ballsToHold - ballsHeld
        return if (remaining < 0) 0 else remaining
    }

    /**
     * Handle the ball hold devices losing a ball.
     */
    private fun lostBall(device: BallDevice, balls: Int): Map<String, Int> {
        if (ballsHeld > 0) {
            ballsHeld -= balls
            infoLog("Ball device $device lost $balls balls, hold now has $ballsHeld balls held")
        } else {
            infoLog("Ball device $device lost $balls balls but hold is not holding. Doing nothing.")
        }
        // Do not claim this ball
        return mapOf("balls" to balls)
    }

    /**
     * Handle result of ball_enter event of hold_devices.
     */
    private fun holdBall(device: BallDevice, newBalls: Int, unclaimedBalls: Int): Map<String, Int> {
        // If full do not take any balls
        if (isFull()) {
            debugLog("Cannot hold balls. Hold is full.")
            return mapOf("unclaimed_balls" to unclaimedBalls)
        }

        // If there are no balls do not claim anything
        if (unclaimedBalls <= 0) {
            return mapOf("unclaimed_balls" to unclaimedBalls)
        }

        val capacity = remainingSpaceInHold()
        // Take ball up to capacity limit
        val ballsToHold = if (unclaimedBalls > capacity) capacity else unclaimedBalls

        ballsHeld += ballsToHold
        debugLog("Held $ballsToHold balls")

        // Post event for ball capture
        machine.events.post(
            "ball_hold_${name}_held_ball",
            mapOf(
                "balls_held" to ballsToHold,
                "total_balls_held" to ballsHeld
            )
        )
        /**
         * Event: ball_hold_(name)_held_ball
         *
         * The ball hold device (name) has just held additional ball(s).
         *
         * Args:
         *   balls_held: The number of new balls just held
         *   total_balls_held: The current total number of balls this device has held
         */

        // Check if we are full now and post event if yes
        if (isFull()) {
            machine.events.post(
                "ball_hold_${name}_full",
                mapOf("balls" to ballsHeld)
            )
            /**
             * Event: ball_hold_(name)_full
             *
             * The ball hold device (name) is now full.
             *
             * Args:
             *   balls: The number of balls currently held in this device
             */
        }

        holdQueue.addLast(Pair(device, unclaimedBalls))

        return mapOf("unclaimed_balls" to (unclaimedBalls - ballsToHold))
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        enable()
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        disable()
    }
}
