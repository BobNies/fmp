package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import java.util.*

/**
 * Represents a logical grouping of score reels in a pinball machine.
 *
 * Multiple individual ScoreReel objects make up the individual digits of this
 * group. This group also has support for the blank zero "inserts" that some
 * machines use.
 */
class ScoreReelGroup(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "score_reel_groups"
    override val collection = "score_reel_groups"
    override val classLabel = "score_reel_group"

    companion object {
        /**
         * If we have at least one score reel group, we need a ScoreReelController.
         */
        @JvmStatic
        fun deviceClassInit(machine: MachineController) {
            // TODO: Create score reel controller when available
            /*
            machine.scoreReelController = ScoreReelController(machine)
            */
        }
    }

    /**
     * Queue for waiting for valid reels.
     */
    var waitForValidQueue: Any? = null

    /**
     * Confirmed reels are showing the right values.
     */
    var valid = true

    /**
     * Event key for unlighting on resync.
     */
    var unlightOnResyncKey: Any? = null

    /**
     * Event key for lighting on valid.
     */
    var lightOnValidKey: Any? = null

    /**
     * List of individual ScoreReel objects that make up this ScoreReelGroup.
     * The number of items in the list corresponds to the number of digits
     * that can be displayed. A value of null indicates a position that is
     * not controlled by a moving reel (like a fake ones digit).
     *
     * Note that this is "backwards," with element 0 representing the ones
     * digit, element 1 representing the tens, etc.
     */
    val reels = mutableListOf<ScoreReel?>()

    /**
     * List of what values the machine desires to have the score reel
     * group set to.
     */
    val desiredValueList = mutableListOf<Int?>()

    /**
     * Holds a list of the next reels that for step advances.
     */
    val advanceQueue = ArrayDeque<Any>()

    /**
     * Boolean attribute that is true when a jump advance is in progress.
     */
    var jumpInProgress = false

    /**
     * Tick task for managing reel advances.
     */
    private var tickTask: Any? = null

    override suspend fun initialize() {
        super.initialize()

        val configReels = config["reels"] as? List<*> ?: emptyList<ScoreReel?>()
        reels.addAll(configReels.map { it as? ScoreReel })
        reels.reverse()  // We want our smallest digit in the 0th element

        val chimes = (config["chimes"] as? MutableList<*>)?.reversed() ?: emptyList<Driver?>()

        for (i in chimes.indices) {
            val chime = chimes[i] as? Driver
            if (chime != null) {
                if (reels.getOrNull(i) == null) {
                    throw IllegalArgumentException("Invalid reel for chime $chime")
                }

                val reel = reels[i]!!
                // TODO: Register event handler when available
                /*
                machine.events.addHandler(
                    "reel_${reel.name}_advancing",
                    { pulseChime(chime) }
                )
                */
            }
        }
    }

    /**
     * Pulse chime.
     */
    private fun pulseChime(chime: Driver) {
        // TODO: Pulse when driver is available
        /*
        chime.pulse()
        */
    }

    /**
     * Reset the score reel group to display the value passed.
     *
     * This method will "jump" the score reel group to display the value
     * that's passed. (Note this "jump" technique means it will just
     * move the reels as fast as it can, and nonsensical values might show up
     * on the reel while the movement is in progress.)
     *
     * This method is used to "reset" a reel group to all zeros at the
     * beginning of a game, and can also be used to reset a reel group that is
     * confused or to switch a reel to the new player's score if multiple
     * players are sharing the same reel group.
     *
     * @param value An integer value of what the new displayed value should be
     */
    fun setValue(value: Int) {
        val valueList = intToReelList(value)

        debugLog("Jumping to $valueList.")

        // Set the new desired value which we'll use to verify the reels land
        // where we want them to
        desiredValueList.clear()
        desiredValueList.addAll(valueList)

        // Loop through the reels one by one
        for ((i, reel) in reels.withIndex()) {
            if (reel != null) {
                val desiredValue = desiredValueList.getOrNull(i) ?: 0
                reel.setDestinationValue(desiredValue)
            }
        }
    }

    /**
     * Return a deferred which will be done when all reels reached their destination.
     */
    suspend fun waitForReady() {
        for (reel in reels) {
            reel?.waitForReady()?.await()
        }
    }

    /**
     * Convert an integer to a list of integers that represent each positional digit.
     *
     * The list returned is in reverse order. (See the example below.)
     *
     * The list returned is customized for this ScoreReelGroup both in terms
     * of number of elements and values of null used to represent blank
     * plastic zero inserts that are not controlled by a score reel unit.
     *
     * For example, if you have a 5-digit score reel group that has 4
     * physical reels in the tens through ten-thousands position and a fake
     * plastic "0" insert for the ones position, if you pass this method a
     * value of 12300, it will return [null, 0, 3, 2, 1]
     *
     * This method will pad shorter ints with zeros, and it will chop off
     * leading digits for ints that are too long. (For example, if you pass a
     * value of 10000 to a ScoreReelGroup which only has 4 digits, the
     * returned list would correspond to 0000, since your score reel unit has
     * rolled over.)
     *
     * @param value The integer value you'd like to convert
     * @return A list containing the values for each corresponding score reel
     */
    fun intToReelList(value: Int): List<Int?> {
        val outputList = mutableListOf<Int?>()

        // Convert our number to a string
        var strValue = value.toString()

        // Pad the string with leading zeros
        strValue = strValue.padStart(reels.size, '0')

        // Slice off excess characters if the value is longer than num of reels
        val trim = strValue.length - reels.size
        if (trim > 0) {
            strValue = strValue.substring(trim)
        }

        // Generate our list with one digit per item
        for (digit in strValue) {
            outputList.add(digit.toString().toInt())
        }

        // Reverse the list so the least significant is first
        outputList.reverse()

        // Replace fake position digits with null
        for (i in outputList.indices) {
            if (reels.getOrNull(i) == null) {
                outputList[i] = null
            }
        }

        return outputList
    }

    /**
     * Light up this ScoreReelGroup based on the 'lights_tag' in its config.
     */
    fun light() {
        debugLog("Turning on Lights")
        val lightsTag = config["lights_tag"] as? String ?: return

        // TODO: Get tagged lights when available
        /*
        for (light in machine.lights.itemsTagged(lightsTag)) {
            light.on()
        }
        */
    }

    /**
     * Turn off the lights for this ScoreReelGroup based on the 'lights_tag' in its config.
     */
    fun unlight() {
        debugLog("Turning off Lights")
        val lightsTag = config["lights_tag"] as? String ?: return

        // TODO: Get tagged lights when available
        /*
        for (light in machine.lights.itemsTagged(lightsTag)) {
            light.off()
        }
        */
    }
}
