package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice

/**
 * Ball lock device which locks balls for a multiball.
 *
 * Features:
 * - Configurable locking strategies (virtual_only, physical_only, min_virtual_physical, no_virtual)
 * - Ball replacement support
 * - Player-specific locked ball tracking
 * - Empty lock devices on ball end option
 * - Ball lost handling
 * - Source device configuration for ball replacement
 *
 * Note: In Python, this uses @DeviceMonitor("locked_balls") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this extends EnableDisableMixin.
 * In Kotlin, we implement enable/disable functionality directly.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class MultiballLock(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "multiball_locks"
    override val collection = "multiball_locks"
    override val classLabel = "multiball_lock"

    /**
     * Lock devices that hold balls.
     */
    private val lockDevices = mutableListOf<BallDevice>()

    /**
     * Source playfield for ball ejections.
     */
    private var sourcePlayfield: Any? = null

    /**
     * Source devices for replacement balls.
     */
    private var sourceDevices: List<BallDevice>? = null

    /**
     * Events to post per device (delayed until ball_entered).
     */
    private val events = mutableMapOf<BallDevice, MutableList<Map<String, Any?>>>()

    /**
     * Locked balls when keep_virtual_ball_count_per_player is false.
     */
    private var _lockedBalls = 0

    /**
     * Player variable name for locked ball count.
     */
    private val playerVarName = "${name}_locked_balls"

    /**
     * Whether the lock is enabled.
     */
    private var _enabled = false

    override suspend fun initialize() {
        super.initialize()

        // Load lock_devices
        lockDevices.clear()
        val configLockDevices = config["lock_devices"] as? List<*> ?: emptyList<Any>()
        for (device in configLockDevices) {
            if (device is BallDevice) {
                lockDevices.add(device)
                events[device] = mutableListOf()
            }
        }

        // TODO: Get source playfield and devices when available
        /*
        sourcePlayfield = config["source_playfield"]
        sourceDevices = config["source_devices"] as? List<BallDevice>
        */

        // TODO: Register event handlers when available
        /*
        machine.events.addHandler("player_turn_starting", ::playerTurnStarting)
        machine.events.addHandler("ball_ending", ::ballEnding)

        for (device in lockDevices) {
            machine.events.addHandler("balldevice_${device.name}_ball_missing", ::lostBall, device = device)
        }
        */
    }

    /**
     * Enable the lock.
     *
     * If the lock is not enabled, no balls will be locked.
     */
    private fun enableLock() {
        debugLog("Enabling...")
        registerHandlers()
        _enabled = true
    }

    /**
     * Handle ball ending.
     */
    private fun ballEnding(queue: Any) {
        val emptyOnBallEnd = config["empty_lock_devices_on_ball_end"] as? Boolean ?: false
        if (emptyOnBallEnd) {
            var totalBallToDrain = 0
            for (device in lockDevices) {
                // TODO: Eject balls when BallDevice is available
                /*
                totalBallToDrain += device.availableBalls
                device.eject(device.availableBalls)
                */
            }

            if (totalBallToDrain > 0) {
                log.info("Ejected $totalBallToDrain balls to empty lock devices. Waiting for them to drain!")
                // TODO: Queue operations when available
                /*
                queue.wait()
                machine.events.addHandler("ball_drain", ::waitForDrain,
                    queue = queue, ballCounter = mutableMapOf("remaining" to totalBallToDrain))
                */
            }
        }
    }

    /**
     * Handle player turn starting.
     */
    private fun playerTurnStarting(queue: Any) {
        // Reset locked balls
        _lockedBalls = 0

        // Check if the lock is physically full and not virtually full and release balls in that case
        if (physicallyRemainingSpace <= 0 && !isVirtuallyFull) {
            log.info("Will release a ball because the lock is physically full but not virtually for the player.")
            // TODO: Eject to next playfield when available
            /*
            lockDevices.firstOrNull()?.eject()
            queue.wait()
            machine.events.addHandler("ball_drain", ::waitForDrain,
                queue = queue, ballCounter = mutableMapOf("remaining" to 1))
            */
        }
    }

    /**
     * Wait for drain handler.
     */
    private fun waitForDrain(queue: Any, ballCounter: MutableMap<String, Int>, balls: Int): Map<String, Int> {
        if (balls <= 0) {
            return mapOf("balls" to balls)
        }

        val remaining = ballCounter["remaining"] ?: 0
        val ballsToIgnore = minOf(remaining, balls)
        ballCounter["remaining"] = remaining - ballsToIgnore
        infoLog("Ignoring $ballsToIgnore drained balls")

        if (ballCounter["remaining"] == 0) {
            debugLog("Ball of lock drained.")
            // TODO: Clear queue and remove handler when available
            /*
            queue.clear()
            machine.events.removeHandlerByEvent("ball_drain", ::waitForDrain)
            */
        }

        return mapOf("balls" to (balls - ballsToIgnore))
    }

    /**
     * Disable the lock.
     *
     * If the lock is not enabled, no balls will be locked.
     */
    private fun disableLock() {
        debugLog("Disabling...")
        unregisterHandlers()
        _enabled = false
    }

    /**
     * Event handler for reset_all_counts event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventResetAllCounts() {
        resetAllCounts()
    }

    /**
     * Reset the locked balls for all players.
     */
    fun resetAllCounts() {
        val strategy = config["locked_ball_counting_strategy"] as? String ?: "virtual_only"
        if (strategy !in listOf("virtual_only", "min_virtual_physical")) {
            throw AssertionError("Count is only tracked per player")
        }
        // TODO: Reset for all players when game is available
        /*
        for (player in machine.game.playerList) {
            player[playerVarName] = 0
        }
        */
    }

    /**
     * Event handler for reset_count_for_current_player event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventResetCountForCurrentPlayer() {
        resetCountForCurrentPlayer()
    }

    /**
     * Reset the locked balls for the current player.
     */
    fun resetCountForCurrentPlayer() {
        val strategy = config["locked_ball_counting_strategy"] as? String ?: "virtual_only"
        if (strategy in listOf("virtual_only", "min_virtual_physical", "no_virtual")) {
            lockedBalls = 0
        } else {
            throw AssertionError("Cannot reset physical balls")
        }
    }

    /**
     * Return the number of locked balls for the current player.
     */
    var lockedBalls: Int
        get() {
            val game = machine.game
            // This is required for the monitor because it will query this variable outside of a game
            // TODO: Remove when issue #893 is fixed
            if (game == null) {
                return 0
            }

            val strategy = config["locked_ball_counting_strategy"] as? String ?: "virtual_only"
            return when (strategy) {
                "virtual_only" -> game.player?.get(playerVarName) as? Int ?: 0
                "min_virtual_physical" -> {
                    val virtual = game.player?.get(playerVarName) as? Int ?: 0
                    minOf(virtual, physicallyLockedBalls)
                }
                "physical_only" -> physicallyLockedBalls
                else -> _lockedBalls
            }
        }
        set(value) {
            val strategy = config["locked_ball_counting_strategy"] as? String ?: "virtual_only"
            when (strategy) {
                "virtual_only", "min_virtual_physical" -> {
                    machine.game?.player?.set(playerVarName, value)
                }
                "no_virtual" -> {
                    _lockedBalls = value
                }
                else -> {
                    throw AssertionError("Cannot write locked_balls for strategy $strategy")
                }
            }
        }

    /**
     * Register ball enter handlers.
     */
    private fun registerHandlers() {
        val priority = (mode?.priority ?: 0) + (config["priority"] as? Int ?: 0)
        val blockingFacility = config["blocking_facility"] as? String

        // Register on ball_enter of lock_devices
        for (device in lockDevices) {
            // TODO: Register event handlers when available
            /*
            machine.events.addHandler(
                "balldevice_${device.name}_ball_enter",
                ::lockBall, device = device, priority = priority,
                blockingFacility = blockingFacility)
            machine.events.addHandler(
                "balldevice_${device.name}_ball_entered",
                ::postEvents, device = device, priority = priority,
                blockingFacility = blockingFacility)
            */
        }
    }

    /**
     * Unregister ball_enter handlers.
     */
    private fun unregisterHandlers() {
        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandler(::lockBall)
        machine.events.removeHandler(::postEvents)
        */
    }

    /**
     * Return true if lock is full.
     */
    val isVirtuallyFull: Boolean
        get() = remainingVirtualSpaceInLock <= 0

    /**
     * Return the remaining capacity of the lock.
     */
    val remainingVirtualSpaceInLock: Int
        get() {
            val ballsToLock = (config["balls_to_lock"] as? Int ?: 3) - lockedBalls
            return if (ballsToLock < 0) 0 else ballsToLock
        }

    /**
     * Return the highest number of balls locked for all players.
     */
    private val maxBallsLockedByAnyPlayer: Int
        get() {
            // TODO: Iterate players when game is available
            /*
            var maxBalls = 0
            for (player in machine.game.playerList) {
                val locked = player[playerVarName] as? Int ?: 0
                if (maxBalls < locked) {
                    maxBalls = locked
                }
            }
            return maxBalls
            */
            return 0
        }

    /**
     * Return the number of physically locked balls.
     */
    private val physicallyLockedBalls: Int
        get() {
            var balls = 0
            for (device in lockDevices) {
                // TODO: Get available balls when BallDevice is available
                /*
                balls += device.availableBalls
                */
            }
            return balls
        }

    /**
     * Return the space in the physical locks.
     */
    private val physicallyRemainingSpace: Int
        get() {
            var balls = 0
            for (device in lockDevices) {
                // TODO: Get capacity and available balls when BallDevice is available
                /*
                balls += device.capacity - device.availableBalls
                */
            }
            return balls
        }

    /**
     * Handle result of the _ball_enter event of lock_devices.
     */
    private fun lockBall(unclaimedBalls: Int, newAvailableBalls: Int, device: BallDevice): Map<String, Int> {
        // If there are no balls do not claim anything
        if (unclaimedBalls <= 0) {
            return mapOf("unclaimed_balls" to unclaimedBalls)
        }

        // MPF will make sure that devices get one event per ball
        assert(unclaimedBalls == 1)

        val game = machine.game
        if (game == null || game.player == null) {
            // Bail out if we are outside of a game
            return mapOf("unclaimed_balls" to unclaimedBalls)
        }

        // If already full do not take any balls
        if (isVirtuallyFull) {
            debugLog("Cannot lock balls. Lock is full.")
            return mapOf("unclaimed_balls" to unclaimedBalls)
        }

        // First take care of virtual ball count in lock
        val capacity = remainingVirtualSpaceInLock
        val ballsToLock = if (unclaimedBalls > capacity) capacity else unclaimedBalls

        val newLockedBalls = lockedBalls + 1
        // Post event for ball capture
        events[device]?.add(
            mapOf(
                "event" to "multiball_lock_${name}_locked_ball",
                "total_balls_locked" to newLockedBalls
            )
        )
        /**
         * Event: multiball_lock_(name)_locked_ball
         *
         * The multiball lock device (name) has just locked one additional ball.
         *
         * Args:
         *   total_balls_locked: The current total number of balls this device has locked
         */

        val strategy = config["locked_ball_counting_strategy"] as? String ?: "virtual_only"
        if (strategy != "physical_only") {
            lockedBalls = newLockedBalls
        }

        // Now check how many balls we want physically in the lock
        var ballsToLockPhysically = ballsToLock

        if (physicallyRemainingSpace < newAvailableBalls) {
            // We cannot lock if there isn't any space left
            ballsToLockPhysically = 0
            debugLog("Will not keep the ball. Device is full. Remaining space: $physicallyRemainingSpace. Balls to lock: $ballsToLock")
        }

        if (strategy in listOf("virtual_only", "min_virtual_physical") &&
            maxBallsLockedByAnyPlayer < physicallyLockedBalls + newAvailableBalls) {
            // Only keep ball if any player could use it
            debugLog("Will not keep ball because no player could use it. Max locked balls by any player " +
                "is $maxBallsLockedByAnyPlayer and we physically got $physicallyLockedBalls")
            ballsToLockPhysically = 0
        }

        if (strategy == "min_virtual_physical") {
            // Do not lock if the lock would be physically full but not virtually
            val virtualRemaining = (config["balls_to_lock"] as? Int ?: 3) - (game.player?.get(playerVarName) as? Int ?: 0)
            if (physicallyRemainingSpace <= newAvailableBalls && virtualRemaining > 0) {
                debugLog("Will not keep ball because the lock would be physically full but virtually still " +
                    "has space for this player.")
                ballsToLockPhysically = 0
            }
        } else if (strategy != "physical_only" && !isVirtuallyFull &&
            physicallyRemainingSpace <= newAvailableBalls) {
            // Do not lock if the lock would be physically full but not virtually
            ballsToLockPhysically = 0
            debugLog("Will not keep ball because the lock would be physically full but virtually still " +
                "has space for this player.")
        }

        // Check if we are full now and post event if yes
        if ((strategy == "physical_only" && newLockedBalls >= (config["balls_to_lock"] as? Int ?: 3)) ||
            remainingVirtualSpaceInLock == 0) {
            events[device]?.add(
                mapOf(
                    "event" to "multiball_lock_${name}_full",
                    "balls" to newLockedBalls
                )
            )
        }
        /**
         * Event: multiball_lock_(name)_full
         *
         * The multiball lock device (name) is now full.
         *
         * Args:
         *   balls: The number of balls currently locked in this device
         */

        // Schedule eject of new balls for all physically locked balls
        val ballsToReplace = config["balls_to_replace"] as? Int ?: -1
        if (ballsToReplace == -1 || newLockedBalls <= ballsToReplace) {
            infoLog("$newLockedBalls locked balls and $ballsToReplace to replace, requesting $ballsToLockPhysically new balls")
            requestNewBalls(ballsToLockPhysically)
        } else {
            infoLog("$newLockedBalls locked balls exceeds $ballsToReplace to replace, not requesting any balls")
        }

        infoLog("Locked $ballsToLock balls virtually and $ballsToLockPhysically balls physically")

        return mapOf("unclaimed_balls" to (unclaimedBalls - ballsToLockPhysically))
    }

    /**
     * Handle ball lost from device.
     */
    private fun lostBall(device: BallDevice, balls: Int): Map<String, Int> {
        val ballLostAction = config["ball_lost_action"] as? String ?: "none"
        infoLog("Ball device ${device.name} lost $balls balls, $name has $lockedBalls locked balls and action $ballLostAction")
        if (lockedBalls > 0 && ballLostAction == "add_to_play") {
            infoLog("Ball device ${device.name} lost $balls balls, adding to balls_in_play")
            machine.game?.let { it.ballsInPlay += balls }
        }
        // Do not claim the ball
        return mapOf("balls" to balls)
    }

    /**
     * Post events on callback from _ball_entered handler.
     *
     * Events are delayed to this handler because we want the ball device to have accounted for the balls.
     */
    private fun postEvents(device: BallDevice) {
        for (event in events[device] ?: emptyList()) {
            val eventName = event["event"] as? String ?: continue
            machine.events.post(eventName, event.filterKeys { it != "event" })
        }
        events[device]?.clear()
    }

    /**
     * Request new ball to playfield.
     */
    private fun requestNewBalls(balls: Int) {
        var ballsAdded = 0
        // TODO: Eject from source devices when available
        /*
        for (device in sourceDevices ?: emptyList()) {
            val ballsToAdd = maxOf(minOf(device.availableBalls, balls - ballsAdded), 0)
            device.eject(balls = ballsToAdd, target = sourcePlayfield)
            ballsAdded += ballsToAdd
        }

        sourcePlayfield?.addBall(balls = maxOf(balls - ballsAdded, 0))
        */
    }

    /**
     * Enable the device.
     */
    fun enable() {
        enableLock()
    }

    /**
     * Disable the device.
     */
    fun disable() {
        disableLock()
    }
}
