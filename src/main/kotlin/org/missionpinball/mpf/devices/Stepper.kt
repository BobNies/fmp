package org.missionpinball.mpf.devices

import kotlinx.coroutines.*
import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Represents a stepper motor based axis in a pinball machine.
 *
 * Steppers can be homed (calibrated to a zero position), moved to absolute
 * or relative positions, and integrated with ball search. They support both
 * hardware and switch-based homing modes.
 *
 * Note: In Python, this uses @DeviceMonitor("_current_position", "_target_position", "_is_homed") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class Stepper(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "steppers"
    override val collection = "steppers"
    override val classLabel = "stepper"

    /**
     * Hardware platform interface for the stepper.
     */
    private var hwStepper: StepperPlatformInterface? = null

    /**
     * Platform instance.
     */
    private var platform: Any? = null

    /**
     * Target position in user units.
     */
    private var _targetPosition = 0

    /**
     * Current position in user units.
     */
    private var _currentPosition = 0

    /**
     * Target speed in steps per second (null for default speed).
     */
    private var _targetSpeed: Int? = null

    /**
     * Whether ball search is currently active.
     */
    private var ballSearchStarted = false

    /**
     * Target position before ball search started.
     */
    private var ballSearchOldTarget = 0

    /**
     * Whether the stepper has been homed.
     */
    private var _isHomed = false

    /**
     * Event that is set when the stepper should be moving.
     */
    private val isMoving = CompletableDeferred<Unit>()

    /**
     * Background task that manages stepper movements.
     */
    private var moveTask: Job? = null

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    override suspend fun initialize() {
        super.initialize()

        // TODO: Get platform when available
        /*
        platform = machine.getPlatformSections("stepper_controllers", config["platform"])
        platform?.assertHasFeature("steppers")
        */

        // First target is the reset position but we might get an early target during startup via events
        _targetPosition = (config["reset_position"] as? Number)?.toInt() ?: 0

        // Register event handlers for named positions
        val namedPositions = config["named_positions"] as? Map<*, *> ?: emptyMap<String, Any>()
        for ((position, posConfig) in namedPositions) {
            val positionName = position.toString()
            val posConfigMap = posConfig as? Map<*, *> ?: continue
            val event = posConfigMap["event"] as? String ?: continue
            val speed = (posConfigMap["speed"] as? Number)?.toInt()

            // TODO: Register event handler when available
            /*
            machine.events.addHandler(event, ::eventMoveToPosition,
                mapOf("position" to positionName, "speed" to speed))
            */
        }

        // Register event handlers for relative positions
        val relativePositions = config["relative_positions"] as? Map<*, *> ?: emptyMap<String, Any>()
        for ((position, posConfig) in relativePositions) {
            val positionName = position.toString()
            val posConfigMap = posConfig as? Map<*, *> ?: continue
            val event = posConfigMap["event"] as? String ?: continue
            val speed = (posConfigMap["speed"] as? Number)?.toInt()

            // TODO: Register event handler when available
            /*
            machine.events.addHandler(event, ::eventMoveToPosition,
                mapOf("position" to positionName, "speed" to speed, "is_relative" to true))
            */
        }

        // TODO: Configure stepper hardware when platform is available
        /*
        if (!platform?.features?.get("allow_empty_numbers") && config["number"] == null) {
            throw IllegalArgumentException("Stepper must have a number.")
        }

        hwStepper = platform?.configureStepper(
            config["number"],
            config["platform_settings"]
        )
        */

        // Register ball search handlers
        val includeInBallSearch = config["include_in_ball_search"] as? Boolean ?: false
        if (includeInBallSearch) {
            // TODO: Register event handlers when available
            /*
            machine.events.addHandler("ball_search_started", ::ballSearchStart)
            machine.events.addHandler("ball_search_stopped", ::ballSearchStop)
            */
        }

        // Validate homing configuration
        val homingMode = config["homing_mode"] as? String
        val homingSwitch = config["homing_switch"]
        if (homingMode == "switch" && homingSwitch == null) {
            throw IllegalArgumentException(
                "Cannot use homing_mode switch without a homing_switch. " +
                "Please add homing_switch or use homing_mode hardware."
            )
        }

        // Start the movement task
        moveTask = machine.launch {
            run()
        }
    }

    override fun validateAndParseConfig(
        config: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String?
    ): Map<String, Any?> {
        // Expand simple string configs for positions into full config maps
        for (cfgKey in listOf("named_positions", "relative_positions")) {
            val positions = config[cfgKey] as? MutableMap<*, *> ?: continue
            for ((pos, value) in positions) {
                if (value is String) {
                    positions[pos] = mutableMapOf("event" to value)
                }
            }
        }

        val validatedConfig = super.validateAndParseConfig(config, isModeConfig, debugPrefix)

        // TODO: Validate platform settings when platform is available
        /*
        val platform = machine.getPlatformSections(
            "stepper_controllers",
            config["platform"]
        )
        val platformSettings = platform?.validateStepperSection(
            this,
            config["platform_settings"]
        )
        val mutableConfig = validatedConfig.toMutableMap()
        mutableConfig["platform_settings"] = platformSettings
        return mutableConfig
        */

        return validatedConfig
    }

    /**
     * Main run loop that manages stepper movements.
     */
    private suspend fun run() {
        // Wait for switches to be initialized
        // TODO: Wait for event when available
        /*
        machine.events.waitForEvent("init_phase_3")
        */

        val homeOnStartup = config["home_on_startup"] as? Boolean ?: false
        if (homeOnStartup) {
            // First home the stepper
            infoLog("Initializing stepper and homing.")
            home()
        } else {
            infoLog("Initializing stepper but will not home.")
            _isHomed = true
        }

        // Run the loop at least once
        isMoving.complete(Unit)

        while (isActive) {
            // Wait until we should be moving
            infoLog("Resting position is now $_currentPosition. Stepper is ready to move.")
            isMoving.await()

            if (!_isHomed) {
                home()
                postReadyEvent()
                continue
            }

            debugLog("Moving the stepper to target position $_targetPosition, current position $_currentPosition")
            // Store target position in local variable since it may change in the meantime
            val targetPosition = _targetPosition
            val delta = targetPosition - _currentPosition

            if (delta != 0) {
                infoLog("Stepper moving relative $delta to hit target $targetPosition from $_currentPosition")

                // Move stepper
                hwStepper?.moveRelPos(delta, _targetSpeed)

                // Clear the speed override here, in case a subsequent move wants
                // to set one before this one finishes
                _targetSpeed = null

                // Wait for the move to complete
                hwStepper?.waitForMoveCompleted()
            } else {
                infoLog("Got command to move (relative) to $_targetPosition, but already there. Not moving.")
            }

            // Set current position
            _currentPosition = targetPosition

            // Post ready event
            postReadyEvent()
        }
    }

    /**
     * Move stepper to an absolute position.
     *
     * @param position Target position in user units
     * @param speed Optional speed override in steps per second
     */
    private fun moveToAbsolutePosition(position: Int, speed: Int? = null) {
        infoLog("Moving to absolute position $position. Current position: $_currentPosition")

        val posMin = (config["pos_min"] as? Number)?.toInt() ?: Int.MIN_VALUE
        val posMax = (config["pos_max"] as? Number)?.toInt() ?: Int.MAX_VALUE

        if (position in posMin..posMax) {
            _targetPosition = position
            _targetSpeed = speed
            isMoving.complete(Unit)
        } else {
            throw IllegalArgumentException(
                "moveToAbsolutePosition: position $position is beyond limits ($posMin to $posMax)"
            )
        }
    }

    /**
     * Home the stepper axis, resetting the 0 position.
     */
    private suspend fun home() {
        _isHomed = false
        isMoving.complete(Unit)

        val homingMode = config["homing_mode"] as? String ?: "hardware"

        if (homingMode == "hardware") {
            infoLog("Homing stepper using hardware homing command.")
            val homingDirection = config["homing_direction"] as? String ?: "clockwise"
            hwStepper?.home(homingDirection)
            hwStepper?.waitForMoveCompleted()
        } else {
            val homingSwitch = config["homing_switch"]
            infoLog("Homing stepper using switch homing with switch $homingSwitch.")

            // Move the stepper manually
            val homingDirection = config["homing_direction"] as? String ?: "clockwise"
            if (homingDirection == "clockwise") {
                hwStepper?.moveVelMode(1)
            } else {
                hwStepper?.moveVelMode(-1)
            }

            // TODO: Wait for switch when available
            /*
            machine.switchController.waitForSwitch(homingSwitch, onlyOnChange = false)
            */

            hwStepper?.stop()
            hwStepper?.setHomePosition()
        }

        infoLog("Stepper reached home.")

        _isHomed = true
        // Home position is 0
        _currentPosition = 0
    }

    /**
     * Post a ready event when the stepper reaches its target position.
     */
    private fun postReadyEvent() {
        if (!ballSearchStarted) {
            machine.events.post("stepper_${name}_ready", mapOf("position" to _currentPosition))
            /**
             * Event: stepper_(name)_ready
             *
             * The stepper reached its target position and is ready for the next move.
             */
        }
    }

    override fun stopDevice() {
        // A crash during startup may not have initialized the hw_stepper yet
        hwStepper?.stop()
        moveTask?.cancel()
        moveTask = null
    }

    /**
     * Event handler for home event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventHome() {
        _targetPosition = 0
        _isHomed = false
        isMoving.complete(Unit)
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
     * Move to reset position.
     */
    fun reset() {
        val resetPosition = (config["reset_position"] as? Number)?.toInt() ?: 0
        moveToAbsolutePosition(resetPosition)
    }

    /**
     * Event handler for move_to_position event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     *
     * @param position Position to move to (absolute or relative)
     * @param speed Optional speed override
     * @param isRelative Whether the position is relative to current position
     */
    fun eventMoveToPosition(position: Int?, speed: Int? = null, isRelative: Boolean = false) {
        if (position == null) {
            throw IllegalArgumentException("move_to_position event is missing a position.")
        }
        moveToPosition(position, speed, isRelative)
    }

    /**
     * Move stepper to a position.
     *
     * @param position Position to move to (absolute or relative)
     * @param speed Optional speed override
     * @param isRelative Whether the position is relative to current position
     */
    fun moveToPosition(position: Int, speed: Int? = null, isRelative: Boolean = false) {
        infoLog(
            "Stepper at $_currentPosition moving to ${if (isRelative) "relative" else "absolute"} position $position"
        )

        _targetPosition = if (isRelative) _currentPosition + position else position

        if (ballSearchStarted) {
            return
        }

        moveToAbsolutePosition(_targetPosition, speed)
    }

    /**
     * Ball search start handler.
     */
    private fun ballSearchStart() {
        // We do not touch _targetPosition during ball search so we can reset to it later
        ballSearchOldTarget = _targetPosition
        ballSearchStarted = true
        ballSearchGoToMin()
    }

    /**
     * Move to minimum position for ball search.
     */
    private fun ballSearchGoToMin() {
        val ballSearchMin = (config["ball_search_min"] as? Number)?.toInt() ?: 0
        val ballSearchWait = (config["ball_search_wait"] as? Number)?.toLong() ?: 1000L

        moveToAbsolutePosition(ballSearchMin)
        delay.add(ballSearchWait, "ball_search") {
            ballSearchGoToMax()
        }
    }

    /**
     * Move to maximum position for ball search.
     */
    private fun ballSearchGoToMax() {
        val ballSearchMax = (config["ball_search_max"] as? Number)?.toInt() ?: 100
        val ballSearchWait = (config["ball_search_wait"] as? Number)?.toLong() ?: 1000L

        moveToAbsolutePosition(ballSearchMax)
        delay.add(ballSearchWait, "ball_search") {
            ballSearchGoToMin()
        }
    }

    /**
     * Ball search stop handler.
     */
    private fun ballSearchStop() {
        // Stop delay
        delay.remove("ball_search")
        ballSearchStarted = false

        // Move to last position
        _targetPosition = ballSearchOldTarget
        moveToAbsolutePosition(_targetPosition)
    }

    /**
     * Current position in user units.
     */
    val position: Int
        get() = _currentPosition

    /**
     * Target position in user units.
     */
    val targetPosition: Int
        get() = _targetPosition

    /**
     * Whether the stepper has been homed.
     */
    val isHomed: Boolean
        get() = _isHomed
}

/**
 * Platform interface for stepper motor control.
 *
 * TODO: Implement full platform interface when hardware abstraction is available
 */
interface StepperPlatformInterface {
    /**
     * Move relative to current position.
     *
     * @param delta Steps to move (positive or negative)
     * @param speed Optional speed in steps per second
     */
    fun moveRelPos(delta: Int, speed: Int?)

    /**
     * Wait for the current move to complete.
     */
    suspend fun waitForMoveCompleted()

    /**
     * Home the stepper.
     *
     * @param direction Homing direction ("clockwise" or "counterclockwise")
     */
    fun home(direction: String)

    /**
     * Move in velocity mode (constant speed).
     *
     * @param direction 1 for clockwise, -1 for counterclockwise
     */
    fun moveVelMode(direction: Int)

    /**
     * Stop the stepper.
     */
    fun stop()

    /**
     * Set the current position as home (0).
     */
    fun setHomePosition()
}
