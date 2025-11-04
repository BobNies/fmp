package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player
import java.util.*

/**
 * Represents a group of shots in a pinball machine by grouping together multiple Shot class devices.
 *
 * This is used so you can get "group-level" functionality, like shot rotation,
 * shot group completion, etc. This would be used for a group of rollover lanes,
 * a bank of standups, etc.
 *
 * Note: In Python, this uses @DeviceMonitor("common_state", "rotation_enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class ShotGroup(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "shot_groups"
    override val collection = "shot_groups"
    override val classLabel = "shot_group"

    /**
     * Whether rotation is enabled for this shot group.
     */
    var rotationEnabled: Boolean = false

    /**
     * The shot profile used by this group.
     */
    var profile: ShotProfile? = null

    /**
     * The rotation pattern (e.g., ["left", "right"]).
     */
    private var rotationPattern: ArrayDeque<String>? = null

    /**
     * Common state of all shots in the group, or null if they differ.
     */
    var commonState: String? = null

    override fun addControlEventsInMode(mode: Mode) {
        // Remove enable here - shot groups don't use default enable
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        checkForComplete()

        val shots = config["shots"] as? List<*> ?: emptyList<Any>()
        if (shots.isNotEmpty()) {
            // TODO: Get profile from first shot when Shot device is available
            /*
            profile = (shots[0] as? Shot)?.profile
            val rotPattern = profile?.config?.get("rotation_pattern") as? List<*>
            if (rotPattern != null) {
                rotationPattern = ArrayDeque(rotPattern.map { it.toString() })
            }
            */
        }

        rotationEnabled = (config["enable_rotation_events"] as? List<*>)?.isEmpty() ?: true

        // TODO: Register event handlers when available
        /*
        for (shot in shots) {
            if (shot is Shot) {
                machine.events.addHandler("${shot.name}_hit", ::hit, shot = shot.name)
                machine.events.addHandler("player_shot_${shot.name}", ::checkForComplete)
            }
        }
        */
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandler(::hit)
        machine.events.removeHandler(::checkForComplete)
        */
    }

    /**
     * Return common state if all shots in this group are in the same state.
     *
     * Will return null otherwise.
     */
    fun getCommonState(): String? {
        val shots = config["shots"] as? List<*> ?: return null
        if (shots.isEmpty()) return null

        // TODO: Get state from shots when Shot device is available
        /*
        val firstShot = shots[0] as? Shot ?: return null
        val state = firstShot.stateName

        for (shot in shots) {
            if (shot is Shot && state != shot.stateName) {
                // Shots do not have a common state
                return null
            }
        }

        return state
        */
        return null
    }

    /**
     * Check if all shots in this group are in the same state.
     */
    private fun checkForComplete() {
        val state = getCommonState()
        if (state == commonState) {
            return
        }

        commonState = state

        if (state == null) {
            // Shots do not have a common state
            return
        }

        // If we reached this point we got a common state
        debugLog("Shot group is complete with state: $state")

        machine.events.post("${name}_complete", mapOf("state" to state))
        /**
         * Event: (name)_complete
         *
         * All the member shots in the shot group called (name) are in the same state.
         *
         * Args:
         *   state: Name of the common state of all shots
         */

        machine.events.post("${name}_${state}_complete")
        /**
         * Event: (name)_(state)_complete
         *
         * All the member shots in the shot group called (name) are in the same state named (state).
         */
    }

    /**
     * Event handler for enable control event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    override fun enable() {
        // Enable all member shots
        val shots = config["shots"] as? List<*> ?: return
        // TODO: Enable shots when Shot device is available
        /*
        for (shot in shots) {
            (shot as? Shot)?.enable()
        }
        */
    }

    /**
     * Event handler for disable control event.
     *
     * TODO: Add @EventHandler(3) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    override fun disable() {
        // Disable all member shots
        val shots = config["shots"] as? List<*> ?: return
        // TODO: Disable shots when Shot device is available
        /*
        for (shot in shots) {
            (shot as? Shot)?.disable()
        }
        */
    }

    /**
     * Event handler for reset control event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventReset() {
        reset()
    }

    /**
     * Reset all member shots.
     */
    fun reset() {
        val shots = config["shots"] as? List<*> ?: return
        // TODO: Reset shots when Shot device is available
        /*
        for (shot in shots) {
            (shot as? Shot)?.reset()
        }
        */
    }

    /**
     * Event handler for restart control event.
     *
     * TODO: Add @EventHandler(4) annotation when event system is fully integrated
     */
    fun eventRestart() {
        restart()
    }

    /**
     * Restart all member shots.
     */
    fun restart() {
        val shots = config["shots"] as? List<*> ?: return
        // TODO: Restart shots when Shot device is available
        /*
        for (shot in shots) {
            (shot as? Shot)?.restart()
        }
        */
    }

    /**
     * One of the member shots in this shot group was hit.
     *
     * @param advancing Boolean of whether the state is advancing
     * @param shot Name of the shot that was hit
     * @param kwargs Additional arguments including profile and state
     */
    private fun hit(advancing: Boolean, shot: String, kwargs: Map<String, Any>) {
        machine.events.post("${name}_hit", mapOf("shot" to shot))
        /**
         * Event: (name)_hit
         *
         * A member shot in the shot group called (name) has been hit.
         */

        val state = kwargs["state"] as? String ?: return
        machine.events.post("${name}_${state}_hit", mapOf("shot" to shot))
        /**
         * Event: (name)_(state)_hit
         *
         * A member shot with state (state) in the shot group (name) has been hit.
         */
    }

    /**
     * Event handler for enable_rotation control event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventEnableRotation() {
        enableRotation()
    }

    /**
     * Enable shot rotation.
     *
     * If disabled, rotation events do not actually rotate the shots.
     */
    fun enableRotation() {
        debugLog("Enabling rotation")
        rotationEnabled = true
    }

    /**
     * Event handler for disable_rotation control event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventDisableRotation() {
        disableRotation()
    }

    /**
     * Disable shot rotation.
     *
     * If disabled, rotation events do not actually rotate the shots.
     */
    fun disableRotation() {
        debugLog("Disabling rotation")
        rotationEnabled = false
    }

    /**
     * Event handler for rotate control event.
     *
     * TODO: Add @EventHandler(4) annotation when event system is fully integrated
     */
    fun eventRotate(direction: String? = null) {
        rotate(direction)
    }

    /**
     * Rotate (or "shift") the state of all the shots in this group.
     *
     * This is used for things like lane change, where hitting the flipper button
     * shifts all the states of the shots in the group to the left or right.
     *
     * This method actually transfers the current state of each shot profile to
     * the left or the right, and the shot on the end rolls over to the target
     * on the other end.
     *
     * @param direction String that specifies whether the rotation direction is to
     *                  the left or right. Values are "right" or "left". Default of
     *                  null will cause the shot group to rotate in the direction as
     *                  specified by the rotation_pattern.
     *
     * Note that this shot group must be enabled, and rotation_events for this
     * shot group must both be enabled for the rotation events to work.
     */
    fun rotate(direction: String? = null) {
        if (!rotationEnabled) {
            debugLog("Received rotation request. Rotation Enabled: $rotationEnabled. Will NOT rotate")
            return
        }

        // TODO: Implement rotation when Shot device is available
        /*
        // shot_state_list is deque of states
        val shotStateList = ArrayDeque<Any>()

        val shots = config["shots"] as? List<*> ?: return
        val shotsToRotate = mutableListOf<Shot>()
        for (shot in shots) {
            if (shot is Shot && shot.canRotate) {
                shotsToRotate.add(shot)
                shotStateList.add(shot.state)
            }
        }

        // Figure out which direction we're going to rotate
        var rotateDirection = direction
        if (rotateDirection == null) {
            rotateDirection = rotationPattern?.first()
            rotationPattern?.let { pattern ->
                val first = pattern.removeFirst()
                pattern.addLast(first)
            }
            debugLog("Since no direction was specified, pulling from rotation pattern: '$rotateDirection'")
        }

        // Rotate that list
        if (rotateDirection?.lowercase() in listOf("right", "r")) {
            val last = shotStateList.removeLast()
            shotStateList.addFirst(last)
        } else {
            val first = shotStateList.removeFirst()
            shotStateList.addLast(first)
        }

        // Step through all our shots and update their states
        for ((i, shot) in shotsToRotate.withIndex()) {
            shot.jump(state = shotStateList[i], force = true)
        }
        */
    }

    /**
     * Event handler for rotate_right control event.
     *
     * TODO: Add @EventHandler(8) annotation when event system is fully integrated
     */
    fun eventRotateRight() {
        rotateRight()
    }

    /**
     * Rotate the state of the shots to the right.
     *
     * This method is the same as calling rotate("right")
     */
    fun rotateRight() {
        rotate(direction = "right")
    }

    /**
     * Event handler for rotate_left control event.
     *
     * TODO: Add @EventHandler(7) annotation when event system is fully integrated
     */
    fun eventRotateLeft() {
        rotateLeft()
    }

    /**
     * Rotate the state of the shots to the left.
     *
     * This method is the same as calling rotate("left")
     */
    fun rotateLeft() {
        rotate(direction = "left")
    }
}
