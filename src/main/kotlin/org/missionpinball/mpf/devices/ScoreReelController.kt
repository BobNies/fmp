package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.LogMixin
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.logging.LoggingTarget

/**
 * Overall controller that is in charge of and manages the score reels in a pinball machine.
 *
 * The main thing this controller does is keep track of how many
 * ScoreReelGroups there are in the machine and how many players there are,
 * as well as maps the current player to the proper score reel.
 *
 * This controller is also responsible for working around broken
 * ScoreReelGroups and "stacking" and switching out players when there are
 * multiple players per ScoreReelGroup.
 *
 * Known limitations of this module:
 *  - Assumes all score reels include a zero value.
 *  - Assumes all score reels count up or down by one.
 *  - Assumes all score reels map their displayed value to their stored
 *    value in a 1:1 way. (i.e. value[0] displays 0, value[5] displays 5, etc.)
 *  - Currently this module only supports "incrementing" reels (i.e.
 *    counting up). Decrementing support will be added in the future.
 */
class ScoreReelController(
    private val machine: MachineController
) : LogMixin {

    companion object {
        const val CONFIG_NAME = "score_reel_controller"
    }

    override val loggingTarget = LoggingTarget("ScoreReelController", this)

    /**
     * Pointer to the active ScoreReelGroup for the current player.
     */
    var activeScoreReelGroup: ScoreReelGroup? = null

    /**
     * This is a map of ScoreReelGroup objects which corresponds to player number.
     * The first element [1] in this map is the first player (which is player number [1],
     * the next one is the next player, etc.
     */
    private val playerToScoreReelMap = mutableMapOf<Int, ScoreReelGroup>()

    /**
     * This map tracks which player is active on each reel.
     * Every reel can only have one active player but multiple players can share a reel.
     */
    private val activeReelPlayerMap = mutableMapOf<ScoreReelGroup, Int>()

    init {
        debugLog("Loading the ScoreReelController")

        // TODO: Register event handlers when available
        /*
        // Switch the active score reel group and reset it (if needed)
        machine.events.addHandler("player_turn_started", ::rotatePlayer)

        // Receive notification of score changes
        machine.events.addHandler("player_score", ::scoreChange)

        // Receive notifications of game starts to reset the reels
        machine.events.addAsyncHandler("game_starting", ::gameStarting)

        // Receive notifications of game ends to reset the reels
        machine.events.addHandler("game_ending", ::gameEnding)

        // Need to hook this in case reels aren't done when ball ends
        machine.events.addAsyncHandler("ball_ending", ::ballEnding, priority = 900)
        */
    }

    /**
     * Start a new player's turn.
     *
     * The main purpose of this method is to map the current player to their
     * ScoreReelGroup in the backbox. It will do this by comparing the length of
     * the map which holds those mappings (`playerToScoreReelMap`) to the length
     * of the list of players. If the player list is longer that means we don't
     * have a ScoreReelGroup for that player.
     *
     * In that case it will check the tags of the ScoreReelGroups to see if one
     * of them is tagged with playerX which corresponds to this player. If not
     * then it will pick the next free one. If there are none free, then it will
     * "double up" that player on an existing one which means the same Score
     * Reels will be used for both players, and they will reset themselves
     * automatically between players.
     */
    private fun rotatePlayer() {
        // Unlight active score reel group
        activeScoreReelGroup?.unlight()

        val player = machine.game?.player ?: return
        activeScoreReelGroup = playerToScoreReelMap[player.number]

        activeScoreReelGroup?.let { group ->
            activeReelPlayerMap[group] = player.number

            debugLog("Mapping Player ${player.number} to ScoreReelGroup '${group.name}'")

            // Make sure this score reel group is showing the right score
            debugLog("Current player's score: ${player.score}")
            group.setValue(player.score)

            group.light()
        }
    }

    /**
     * Handle score changes and add the score increase to the current active ScoreReelGroup.
     *
     * This method is the handler for the score change event, so it's called automatically.
     *
     * @param value Integer value of the new score
     * @param change Change compared to the previous score
     * @param playerNum Player number of the player whose score changed
     */
    private fun scoreChange(value: Int, change: Int, playerNum: Int) {
        // Get score reel group for player
        val scoreReelGroup = playerToScoreReelMap[playerNum] ?: return

        // Check if it is currently dedicated to that player
        if (activeReelPlayerMap[scoreReelGroup] == playerNum) {
            // Set value
            scoreReelGroup.setValue(value)
        }
    }

    /**
     * Reset the score reels when a new game starts.
     *
     * This is a queue event so it doesn't allow the game start to continue until it's done.
     */
    private suspend fun gameStarting() {
        val game = machine.game ?: return

        // Calculate a player <-> reel mapping
        for (playerNum in 1..game.maxPlayers) {
            // TODO: Get tagged score reel groups when available
            /*
            val reel = machine.scoreReelGroups.itemsTagged("player$playerNum")
            if (reel.isNotEmpty()) {
                playerToScoreReelMap[playerNum] = reel[0]
                if (reel[0] !in activeReelPlayerMap) {
                    activeReelPlayerMap[reel[0]] = playerNum
                }
            } else {
                log.warning("Did not find a score reel for player $playerNum. Did you tag a reel with \"player$playerNum\"? Will reuse player1")
                val reel1 = machine.scoreReelGroups.itemsTagged("player1")
                if (reel1.isEmpty()) {
                    throw AssertionError("Need a score reel group tagged \"player1\"")
                }
                playerToScoreReelMap[playerNum] = reel1[0]
            }
            */
        }

        // TODO: Set all score reel groups to 0 and wait when available
        /*
        for (scoreReelGroup in machine.scoreReelGroups.values()) {
            scoreReelGroup.setValue(0)
            scoreReelGroup.waitForReady().await()
        }
        */
    }

    /**
     * Reset controller when game ends.
     */
    private fun gameEnding() {
        activeScoreReelGroup?.unlight()
        activeScoreReelGroup = null
        playerToScoreReelMap.clear()
    }

    /**
     * Wait for all score reel groups to be ready at ball end.
     */
    private suspend fun ballEnding() {
        // TODO: Wait for all score reel groups when available
        /*
        for (scoreReelGroup in machine.scoreReelGroups.values()) {
            scoreReelGroup.waitForReady().await()
        }
        */
    }
}
