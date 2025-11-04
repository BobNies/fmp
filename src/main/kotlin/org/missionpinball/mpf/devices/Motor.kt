package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A motor which can be controlled using drivers.
 *
 * Motors can move to specific positions indicated by position switches.
 * They support one-directional movement (always moves in the same direction)
 * or bi-directional movement (can move left or right).
 *
 * Note: In Python, this uses @DeviceMonitor("_move_direction", "_target_position",
 * "_last_position") decorator. In Kotlin, monitoring will be implemented when
 * device monitoring is available.
 */
class Motor(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "motors"
    override val collection = "motors"
    override val classLabel = "motor"

    /**
     * Target position the motor is moving to.
     */
    private var _targetPosition: String? = null

    /**
     * Last known position of the motor.
     */
    private var _lastPosition: String? = null

    /**
     * Type of motor ("one_direction" or "two_directions").
     */
    var type: String? = null

    /**
     * Current movement direction ("stopped", "left", or "right").
     */
    private var _moveDirection = "stopped"

    override suspend fun initialize() {
        super.initialize()

        _targetPosition = config["reset_position"] as? String

        val resetPosition = config["reset_position"] as? String
        val positionSwitches = config["position_switches"] as? Map<*, *> ?: emptyMap<String, Any>()

        if (resetPosition != null && !positionSwitches.containsKey(resetPosition)) {
            raiseConfigError(
                "Reset position $resetPosition not in positions $positionSwitches", 1
            )
        }

        val motorLeftOutput = config["motor_left_output"]
        val motorRightOutput = config["motor_right_output"]

        if (motorLeftOutput == null && motorRightOutput == null) {
            raiseConfigError("Need either motor_left_output or motor_right_output", 2)
        }

        if (motorLeftOutput != null && motorLeftOutput == motorRightOutput) {
            raiseConfigError("motor_left_output and motor_right_output need to be different", 3)
        }

        type = if (motorLeftOutput != null && motorRightOutput != null) {
            "two_directions"
        } else {
            "one_direction"
        }

        // Add switch handlers for position switches
        for ((position, switchObj) in positionSwitches) {
            // TODO: Add switch handler when switch system is fully available
            /*
            val switch = switchObj as Switch
            machine.switchController.addSwitchHandlerObj(
                switch,
                ::updatePosition,
                callbackKwargs = mapOf("position" to position)
            )
            */
        }

        // Add handlers for go_to_position events
        val goToPositions = config["go_to_position"] as? Map<*, *> ?: emptyMap<String, String>()
        for ((event, position) in goToPositions) {
            if (!positionSwitches.containsKey(position)) {
                raiseConfigError("Invalid position $position in go_to_position", 4)
            }

            // TODO: Add event handler when event system is fully available
            /*
            machine.events.addHandler(event as String) {
                eventGoToPosition(position as String)
            }
            */
        }

        if (config["include_in_ball_search"] as? Boolean == true) {
            // TODO: Add ball search handlers when event system is fully available
            /*
            machine.events.addHandler("ball_search_started", ::ballSearchStart)
            machine.events.addHandler("ball_search_stopped", ::ballSearchStop)
            */
        }
    }

    /**
     * Verify that at most one position switch is active.
     *
     * @return True if validation passed, false otherwise
     */
    private fun validateLastPosition(): Boolean {
        val positionSwitches = config["position_switches"] as? Map<*, *> ?: emptyMap<String, Any>()

        val activeSwitches = positionSwitches.entries.filter { (_, switchObj) ->
            // TODO: Check switch state when Switch is available
            /*
            (switchObj as? Switch)?.state == true
            */
            false
        }

        if (activeSwitches.size > 1) {
            warningLog(
                "Found ${activeSwitches.size} active position switches: $activeSwitches. " +
                "There should be only one position switch active at a time."
            )

            // TODO: Add service alert when service controller is available
            /*
            machine.service.addTechnicalAlert(
                this,
                "Multiple position switches are active: $activeSwitches. Verify switches."
            )
            */

            return false
        }

        return true
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
     * Go to reset position.
     */
    fun reset() {
        val resetPosition = config["reset_position"] as? String
        if (resetPosition != null) {
            goToPosition(resetPosition)
        }
    }

    /**
     * Event handler for go_to_position event.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     *
     * @param position Position to move to
     */
    fun eventGoToPosition(position: String?) {
        if (position == null) {
            throw IllegalArgumentException("Got go_to_position event without position.")
        }
        goToPosition(position)
    }

    /**
     * Move motor to a specific position.
     *
     * @param position Target position name
     */
    fun goToPosition(position: String) {
        infoLog("Moving motor to position $position")
        _targetPosition = position
        moveToPosition(position)
    }

    /**
     * Internal method to move motor to position.
     *
     * @param position Target position name
     */
    private fun moveToPosition(position: String) {
        if (!validateLastPosition()) {
            warningLog("Will not move motor because multiple position switches are active.")
            stopMotor()
            return
        }

        val positionSwitches = config["position_switches"] as? Map<*, *> ?: emptyMap<String, Any>()
        val switch = positionSwitches[position]

        // Check if we are already in this position
        // TODO: Check switch state when Switch is available
        val isActive = false // (switch as? Switch)?.state ?: false

        if (isActive) {
            // Already in position
            reachedPosition(position)
            stopMotor()
        } else {
            if (type == "two_directions") {
                val assumedPosition = if (_lastPosition != null) {
                    _lastPosition
                } else {
                    // Try to determine position from active switches
                    val activePositions = positionSwitches.entries.filter { (_, switchObj) ->
                        // TODO: Check switch state
                        false // (switchObj as? Switch)?.state ?: false
                    }.map { it.key as String }

                    if (activePositions.size == 1) {
                        debugLog("Assuming position based on switches to be ${activePositions[0]}")
                        activePositions[0]
                    } else {
                        null
                    }
                }

                val positionList = positionSwitches.keys.toList()
                val resetPosition = config["reset_position"] as? String

                when {
                    assumedPosition == null && positionList.indexOf(resetPosition) == 0 -> {
                        moveLeft()
                    }
                    assumedPosition == null -> {
                        moveRight()
                    }
                    positionList.indexOf(assumedPosition) > positionList.indexOf(position) -> {
                        moveLeft()
                    }
                    else -> {
                        moveRight()
                    }
                }
            } else {
                // One direction mode - just start motor
                if (config["motor_left_output"] != null) {
                    moveLeft()
                } else {
                    moveRight()
                }
            }
        }
    }

    /**
     * Handle that motor reached a certain position.
     *
     * @param position Position name
     */
    private fun updatePosition(position: String) {
        val firstKnownPosition = _lastPosition == null

        if (!validateLastPosition()) {
            warningLog("Will stop motor because multiple position switches are active.")
            stopMotor()
            _lastPosition = null
            return
        }

        _lastPosition = position

        if (position == _targetPosition) {
            reachedPosition(position)
        } else {
            debugLog("Motor is at position $position")

            if (type == "two_directions") {
                val positionList = (config["position_switches"] as? Map<*, *>)?.keys?.toList()
                    ?: emptyList<String>()
                val lastIndex = positionList.indexOf(_lastPosition)
                val targetIndex = positionList.indexOf(_targetPosition)

                // Special case: initial position was unknown and we reached our first position
                // We might have moved in the wrong direction, so correct this now
                if (firstKnownPosition && _moveDirection == "right" && lastIndex > targetIndex) {
                    moveLeft()
                } else if (firstKnownPosition && _moveDirection == "left" && lastIndex < targetIndex) {
                    moveRight()
                } else if (lastIndex == 0 || lastIndex == positionList.size - 1) {
                    warningLog("Motor hit end switch $position unexpectedly. Stopping motor.")
                    stopMotor()
                }
            }
        }
    }

    /**
     * Handle that motor reached its target position.
     *
     * @param position Position name
     */
    private fun reachedPosition(position: String) {
        infoLog("Motor reached position $position. Stopping motor.")
        machine.events.post("motor_${name}_reached_$position")
        stopMotor()
    }

    /**
     * Stop the motor.
     */
    private fun stopMotor() {
        val motorLeftOutput = config["motor_left_output"] as? DigitalOutput
        val motorRightOutput = config["motor_right_output"] as? DigitalOutput

        motorLeftOutput?.disable()
        motorRightOutput?.disable()

        _moveDirection = "stopped"
    }

    /**
     * Move motor to the right.
     */
    private fun moveRight() {
        val motorLeftOutput = config["motor_left_output"] as? DigitalOutput
        val motorRightOutput = config["motor_right_output"] as? DigitalOutput

        motorLeftOutput?.disable()
        motorRightOutput?.enable()

        _moveDirection = "right"
    }

    /**
     * Move motor to the left.
     */
    private fun moveLeft() {
        val motorLeftOutput = config["motor_left_output"] as? DigitalOutput
        val motorRightOutput = config["motor_right_output"] as? DigitalOutput

        motorRightOutput?.disable()
        motorLeftOutput?.enable()

        _moveDirection = "left"
    }

    /**
     * Ball search started - stop motor and enable one output.
     */
    private fun ballSearchStart() {
        stopMotor()

        // Simply enable motor. Will move to old position afterwards.
        val motorLeftOutput = config["motor_left_output"] as? DigitalOutput
        val motorRightOutput = config["motor_right_output"] as? DigitalOutput

        if (motorLeftOutput != null) {
            motorLeftOutput.enable()
        } else {
            motorRightOutput?.enable()
        }
    }

    /**
     * Ball search stopped - move to last target position.
     */
    private fun ballSearchStop() {
        // Move to last target position
        _targetPosition?.let { moveToPosition(it) }
    }
}

/**
 * Event: motor_(name)_reached_(position)
 *
 * A motor device called (name) reached position (position).
 */
