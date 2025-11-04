package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player

/**
 * An extra ball which can be awarded once per player.
 *
 * Extra balls can be lit (making them available for collection) or awarded
 * directly. They track how many times they've been awarded per player and
 * support maximum awards per game. Extra balls can optionally be members
 * of extra ball groups for coordinated management.
 *
 * Note: In Python, this uses @DeviceMonitor("enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class ExtraBall(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "extra_balls"
    override val collection = "extra_balls"
    override val classLabel = "extra_ball"

    /**
     * Current player.
     */
    var player: Player? = null

    /**
     * The ExtraBallGroup this ExtraBall belongs to, or null.
     */
    var group: ExtraBallGroup? = null

    /**
     * Player variable name for tracking awards.
     */
    private val playerVarName = "extra_ball_${name}_num_awarded"

    /**
     * Whether this extra ball is enabled.
     *
     * This takes into consideration the enabled setting plus the max balls
     * per game setting.
     */
    val enabled: Boolean
        get() = isOkToAward()

    override suspend fun initialize() {
        super.initialize()
        group = config["group"] as? ExtraBallGroup
    }

    /**
     * Event handler for light control event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventLight() {
        light()
    }

    /**
     * Light an extra ball for potential collection by the player.
     *
     * Lighting an extra ball will immediately increase count against the
     * max_per_game setting, even if the extra ball is a member of a
     * group that's disabled or if the player never actually collects the
     * extra ball.
     *
     * Note that this only really does anything if this extra ball is a
     * member of a group.
     */
    fun light() {
        if (isOkToLight()) {
            machine.events.post("extra_ball_${name}_lit")
            /**
             * Event: extra_ball_(name)_lit
             *
             * The extra ball called (name) has just been lit.
             */

            group?.light()
        } else {
            awardDisabled()
        }
    }

    /**
     * Event handler for award control event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventAward() {
        award()
    }

    /**
     * Award extra ball to player (if enabled).
     */
    fun award() {
        if (isOkToAward()) {
            val currentPlayer = player ?: return
            val currentCount = currentPlayer.get(playerVarName) as? Int ?: 0
            currentPlayer.set(playerVarName, currentCount + 1)

            machine.events.post("extra_ball_${name}_awarded")
            /**
             * Event: extra_ball_(name)_awarded
             *
             * The extra ball called (name) has just been awarded.
             */

            val currentGroup = group
            if (currentGroup != null) {
                currentGroup.award()
            } else {
                // If this EB is not in a group, handle directly
                val extraBalls = (currentPlayer.get("extra_balls") as? Int ?: 0) + 1
                currentPlayer.set("extra_balls", extraBalls)

                machine.events.post("extra_ball_awarded")
                /**
                 * Event: extra_ball_awarded
                 *
                 * An extra ball has just been awarded.
                 */
            }
        } else {
            // EB cannot be awarded
            awardDisabled()
        }
    }

    /**
     * Check whether this extra ball can be lit.
     *
     * This method takes into consideration whether this extra ball is
     * enabled, and, if this extra ball is a member of a group, whether the
     * group is enabled and will allow an additional extra ball to be lit.
     *
     * @return true if the extra ball can be lit
     */
    fun isOkToLight(): Boolean {
        if (!isOkToAward()) {
            return false
        }

        val currentGroup = group
        return if (currentGroup != null) {
            currentGroup.isOkToLight()
        } else {
            true
        }
    }

    /**
     * Check whether this extra ball can be awarded.
     *
     * This method takes into consideration whether this extra ball is
     * enabled, whether the max_per_game has been exceeded, and, if this
     * extra ball is a member of a group, whether the group is enabled and
     * will allow an additional extra ball to be awarded.
     *
     * @return true if the extra ball can be awarded
     */
    fun isOkToAward(): Boolean {
        val isEnabled = config["enabled"] as? Boolean ?: true
        val currentPlayer = player

        if (!isEnabled || currentPlayer == null) {
            return false
        }

        val currentGroup = group
        if (currentGroup != null && !currentGroup.enabled) {
            return false
        }

        val maxPerGame = config["max_per_game"] as? Int
        if (maxPerGame != null) {
            val numAwarded = currentPlayer.get(playerVarName) as? Int ?: 0
            if (maxPerGame <= numAwarded) {
                return false
            }
        }

        return true
    }

    /**
     * Post events indicating the extra ball award is disabled.
     */
    private fun awardDisabled() {
        machine.events.post("extra_ball_award_disabled")
        /**
         * Event: extra_ball_award_disabled
         *
         * The award for an extra ball has just been disabled.
         */

        machine.events.post("extra_ball_${name}_award_disabled")
        /**
         * Event: extra_ball_(name)_award_disabled
         *
         * The award for the extra ball called (name) has just been disabled.
         */

        // Still need to send this even if EBs are disabled since we
        // want to post the group disabled event
        group?.awardDisabled()
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        this.player = player

        if (!player.isPlayerVar(playerVarName)) {
            player.set(playerVarName, 0)
        }
        /**
         * Player variable: extra_ball_(name)_awarded
         *
         * The number of times this extra ball has been awarded to the
         * player in this game. Note that the default max is one (meaning that
         * each extra ball can be awarded once per game), so this value will only
         * be 0 or 1 unless you change the max setting for this extra ball.
         */
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        player = null
    }
}

/**
 * Extra ball group interface.
 *
 * TODO: Implement full extra ball group when available
 */
interface ExtraBallGroup {
    val name: String
    val enabled: Boolean

    fun light()
    fun award()
    fun awardDisabled()
    fun isOkToLight(): Boolean
}
