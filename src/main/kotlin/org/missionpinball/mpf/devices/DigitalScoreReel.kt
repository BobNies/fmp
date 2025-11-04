package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A digital (image-based) score reel.
 *
 * This device maps score values to display frames, allowing traditional
 * score reel displays to be shown on modern displays using images.
 */
class DigitalScoreReel(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "digital_score_reels"
    override val collection = "digital_score_reels"
    override val classLabel = "digital_score_reel"

    /**
     * Map of characters to frame names.
     */
    private val frames = mutableMapOf<String, String>()

    /**
     * Number of reels (digits) in this score display.
     */
    private var reelCount = 0

    /**
     * Whether to include the player number in event names.
     */
    private var includePlayerNumber = false

    override suspend fun initialize() {
        super.initialize()

        reelCount = (config["reel_count"] as? Number)?.toInt() ?: 0
        includePlayerNumber = config["include_player_number"] as? Boolean ?: false

        // Build frame mapping from configuration
        val framesConfig = config["frames"] as? List<*> ?: emptyList<Map<String, Any>>()
        for (frame in framesConfig) {
            val frameMap = frame as? Map<*, *> ?: continue
            val character = frameMap["character"]?.toString() ?: continue
            val frameName = frameMap["frame"]?.toString() ?: continue
            frames[character] = frameName
        }

        // Register event handler for score updates
        // TODO: Register event handler when available
        /*
        machine.events.addHandler(name, ::postReelValues)
        */
    }

    /**
     * Post reel values as an event when the score changes.
     *
     * @param value The score value to display
     */
    private fun postReelValues(value: Any) {
        // Pad the string up to the necessary number of characters in the reel
        val startValue = config["start_value"]?.toString() ?: "0"
        val score = value.toString().padStart(reelCount, startValue[0])

        // Create a dict of reel name keys to target frame values
        val result = mutableMapOf<String, String>()
        for (i in 0 until reelCount) {
            val character = score.getOrNull(i)?.toString() ?: startValue
            result[(i + 1).toString()] = frames[character] ?: startValue
        }

        // Post the event
        val eventName = if (includePlayerNumber) {
            val playerNumber = machine.game?.player?.number ?: 1
            "score_reel_${name}_player$playerNumber"
        } else {
            "score_reel_$name"
        }

        machine.events.post(eventName, result)
        /**
         * Event: score_reel_(name) or score_reel_(name)_player(num)
         *
         * Posted when the digital score reel should update its display.
         *
         * Args:
         *   1: Frame name for reel position 1
         *   2: Frame name for reel position 2
         *   ... (one argument per reel)
         */
    }
}
