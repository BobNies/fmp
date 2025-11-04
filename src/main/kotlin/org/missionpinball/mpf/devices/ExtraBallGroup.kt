package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Tracks and manages groups of extra balls devices.
 *
 * Extra ball groups track how many extra balls have been awarded per game
 * and per ball, enforce limits, and manage lit extra balls. Multiple extra
 * ball devices can be members of a group for coordinated management.
 *
 * Note: In Python, this uses @DeviceMonitor("enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class ExtraBallGroup(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "extra_ball_groups"
    override val collection = "extra_ball_groups"
    override val classLabel = "extra_ball_group"

    /**
     * Current player.
     */
    var player: Player? = null

    /**
     * Player variable name for tracking per-game awards.
     */
    private val playerVarPerGame = "extra_ball_group_${name}_num_awarded_game"

    /**
     * Player variable name for tracking per-ball awards.
     */
    private val playerVarPerBall = "extra_ball_group_${name}_num_awarded_ball"

    /**
     * Player variable name for tracking lit extra balls.
     */
    private val playerVarNumLit = "extra_ball_group_${name}_num_lit"

    init {
        // Register global event handlers
        // TODO: Register event handlers when available
        /*
        machine.events.addHandler("player_added", ::playerAdded)
        machine.events.addHandler("player_turn_starting", ::playerTurnStarting)
        machine.events.addHandler("player_turn_ending", ::playerTurnEnding)
        machine.events.addHandler("ball_started", ::ballStarted)
        */
    }

    /**
     * Whether this extra ball group is enabled.
     *
     * This attribute considers the enabled setting plus the
     * max balls per game and ball settings.
     */
    val enabled: Boolean
        get() {
            val currentPlayer = player
            val isEnabled = config["enabled"] as? Boolean ?: true

            if (currentPlayer == null || !isEnabled) {
                return false
            }

            val maxPerGame = config["max_per_game"] as? Int
            if (maxPerGame != null) {
                val numAwardedGame = currentPlayer.get(playerVarPerGame) as? Int ?: 0
                if (maxPerGame <= numAwardedGame) {
                    return false
                }
            }

            val maxPerBall = config["max_per_ball"] as? Int
            if (maxPerBall != null) {
                val numAwardedBall = currentPlayer.get(playerVarPerBall) as? Int ?: 0
                if (maxPerBall <= numAwardedBall) {
                    return false
                }
            }

            return true
        }

    /**
     * Called once per player to setup their vars for this group.
     */
    private fun playerAdded(player: Player) {
        player.set(playerVarPerGame, 0)
        player.set(playerVarNumLit, 0)
        player.set(playerVarPerBall, 0)
    }

    /**
     * Reset the num of EBs awarded per ball.
     *
     * We do this on turn start rather than ball start because a player
     * shooting again is technically another ball start even though it's
     * the same ball number.
     */
    private fun playerTurnStarting(player: Player, number: Int) {
        this.player = player
        player.set(playerVarPerBall, 0)
    }

    /**
     * Check if we need to relight the group when ball starts.
     */
    private fun ballStarted(ball: Int, player: Player) {
        val numLit = this.player?.get(playerVarNumLit) as? Int ?: 0
        if (numLit > 0) {
            postLitEvents()
        }
    }

    /**
     * Clear the lit status if lit memory is disabled.
     */
    private fun playerTurnEnding(player: Player, number: Int) {
        val litMemory = config["lit_memory"] as? Boolean ?: true
        if (!litMemory) {
            player.set(playerVarNumLit, 0)
        }

        this.player = null
    }

    /**
     * Check if it's possible to light an extra ball.
     *
     * This method checks to see if the group is enabled and whether the
     * max_lit setting has been exceeded.
     *
     * @return true if the group can light an extra ball
     */
    fun isOkToLight(): Boolean {
        val currentPlayer = player

        if (!enabled || currentPlayer == null) {
            return false
        }

        val maxLit = config["max_lit"] as? Int
        if (maxLit != null) {
            val numLit = currentPlayer.get(playerVarNumLit) as? Int ?: 0
            if (maxLit <= numLit) {
                return false
            }
        }

        return true
    }

    /**
     * Event handler for award_lit control event.
     *
     * TODO: Add @EventHandler(2) annotation when event system is fully integrated
     */
    fun eventAwardLit() {
        awardLit()
    }

    /**
     * Award a lit extra ball.
     *
     * If the player does not have any lit extra balls, this method does
     * nothing.
     */
    fun awardLit() {
        val currentPlayer = player ?: return

        if (!enabled) {
            awardDisabled()
            return
        }

        val numLit = currentPlayer.get(playerVarNumLit) as? Int ?: 0
        if (numLit < 1) {
            return
        }

        currentPlayer.set(playerVarNumLit, numLit - 1)

        val postedUnlitEvents = if (numLit - 1 == 0) {
            postUnlitEvents()
            true
        } else {
            false
        }

        award(postedUnlitEvents)
    }

    /**
     * Event handler for award control event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventAward(postedUnlitEvents: Boolean = false) {
        award(postedUnlitEvents)
    }

    /**
     * Immediately awards an extra ball.
     *
     * This event first checks to make sure the limits of the max extra
     * balls have not been exceeded and that this group is enabled.
     *
     * Note that this method will work even if this group does not have any
     * extra balls or extra balls lit. You can use this to directly award an
     * extra ball.
     *
     * @param postedUnlitEvents Whether unlit events have already been posted
     */
    fun award(postedUnlitEvents: Boolean = false) {
        if (!enabled) {
            awardDisabled()
            return
        }

        machine.events.post("extra_ball_group_${name}_awarded")
        /**
         * Event: extra_ball_group_(name)_awarded
         *
         * An extra ball from this group was just awarded. This is a
         * good event to use to trigger award shows, sounds, etc.
         */

        val currentPlayer = player ?: return

        val numAwardedGame = currentPlayer.get(playerVarPerGame) as? Int ?: 0
        currentPlayer.set(playerVarPerGame, numAwardedGame + 1)

        val numAwardedBall = currentPlayer.get(playerVarPerBall) as? Int ?: 0
        currentPlayer.set(playerVarPerBall, numAwardedBall + 1)

        val extraBalls = (currentPlayer.get("extra_balls") as? Int ?: 0) + 1
        currentPlayer.set("extra_balls", extraBalls)

        machine.events.post("extra_ball_awarded")

        // If this award puts us over the max limits, make sure none are lit
        if (!enabled) {
            currentPlayer.set(playerVarNumLit, 0)
            if (!postedUnlitEvents) {
                postUnlitEvents()
            }
        }
    }

    /**
     * Event handler for light control event.
     *
     * TODO: Add @EventHandler(3) annotation when event system is fully integrated
     */
    fun eventLight() {
        light()
    }

    /**
     * Light the extra ball for possible collection by the player.
     *
     * This method checks that the group is enabled and that the max lit
     * value has not been exceeded. If so, this method will post the extra
     * ball disabled events.
     */
    fun light() {
        if (isOkToLight()) {
            val currentPlayer = player ?: return
            val numLit = currentPlayer.get(playerVarNumLit) as? Int ?: 0
            currentPlayer.set(playerVarNumLit, numLit + 1)

            machine.events.post("extra_ball_group_${name}_lit_awarded")
            /**
             * Event: extra_ball_group_(name)_lit_awarded
             *
             * This event is posted when an extra ball is lit during play.
             * It is NOT posted when a player's turn starts if they have a lit
             * extra ball from their previous turn. Therefore this event is a
             * good event to use for your award slides and shows when a player
             * lights the extra ball, because you don't want to use
             * extra_ball_group_lit because that is also posted when
             * the player's turn starts and you don't want the award show to play
             * again when they're starting their turn.
             */

            postLitEvents()
        } else {
            awardDisabled()
        }
    }

    /**
     * Post events when extra ball is lit.
     */
    private fun postLitEvents() {
        machine.events.post("extra_ball_group_${name}_lit")
        /**
         * Event: extra_ball_group_(name)_lit
         *
         * An extra ball was just lit. This is a good event to use to
         * start your extra ball lit mode, to turn on an extra ball light,
         * to play the "get that extra ball" sound, etc.
         *
         * Note that this event is posted if an extra ball is lit during play
         * and also when a player's turn starts if they have a lit extra ball.
         */
    }

    /**
     * Post events when no more extra balls are lit.
     */
    private fun postUnlitEvents() {
        machine.events.post("extra_ball_group_${name}_unlit")
        /**
         * Event: extra_ball_group_(name)_unlit
         *
         * No more lit extra balls are available for this extra ball group.
         * This is a good event to use as a stop event for your extra ball lit
         * mode or whatever you're using to indicate to the player that an extra
         * ball is available.
         */
    }

    /**
     * Post the events when an extra ball cannot be awarded.
     */
    fun awardDisabled() {
        machine.events.post("extra_ball_group_${name}_award_disabled")
        /**
         * Event: extra_ball_group_(name)_award_disabled
         *
         * Posted when you have the global extra ball settings set to not
         * enable extra balls but where an extra ball would have been awarded.
         * This is a good alternative event to use to score points or whatever
         * else you want to give the player when extra balls are disabled.
         */
    }
}
