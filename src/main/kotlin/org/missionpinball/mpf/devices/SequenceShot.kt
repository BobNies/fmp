package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice
import java.util.*

/**
 * Active sequence data class.
 *
 * @property id Unique ID for this sequence instance
 * @property currentPositionIndex Current position in the sequence
 * @property nextEvent Next event name expected in the sequence
 */
data class ActiveSequence(
    val id: UUID,
    val currentPositionIndex: Int,
    val nextEvent: String
)

/**
 * A device which represents a sequence shot.
 *
 * Sequence shots require hitting multiple switches or triggering multiple events
 * in a specific order within a time limit. Used for things like orbits, ramps,
 * and skill shots that require hitting multiple targets in sequence.
 *
 * Note: In Python, this inherits from both SystemWideDevice and ModeDevice.
 * In Kotlin, it extends SystemWideDevice and includes mode-related functionality directly.
 */
class SequenceShot(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "sequence_shots"
    override val collection = "sequence_shots"
    override val classLabel = "sequence_shot"

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * List of active sequences being tracked.
     */
    private val activeSequences = mutableListOf<ActiveSequence>()

    /**
     * Set of active delay names (for delay switches/events).
     */
    private val activeDelays = mutableSetOf<String>()

    /**
     * List of sequence event names (from switches or events).
     */
    private val sequenceEvents = mutableListOf<String>()

    /**
     * Map of delay event/switch names to their delay times.
     */
    private val delayEvents = mutableMapOf<String, Int>()

    /**
     * Start time of the current sequence (for elapsed time tracking).
     */
    private var startTime: Double? = null

    /**
     * Mode this device is associated with (if in mode).
     */
    var mode: Mode? = null

    /**
     * Whether this device can exist outside of a game.
     */
    val canExistOutsideOfGame: Boolean = true

    /**
     * Called when device is added system-wide.
     */
    suspend fun deviceAddedSystemWide() {
        registerHandlers()
    }

    /**
     * Called when device is loaded in a mode.
     */
    fun deviceLoadedInMode(mode: Mode, player: Player) {
        registerHandlers()
    }

    /**
     * Called when device is removed from a mode.
     */
    fun deviceRemovedFromMode(mode: Mode) {
        removeHandlers()
        resetAllSequences()
        delay.clear()
    }

    override suspend fun initialize() {
        super.initialize()

        val switchSequence = config["switch_sequence"] as? List<*> ?: emptyList<Any>()
        val eventSequence = config["event_sequence"] as? List<*> ?: emptyList<Any>()

        if (switchSequence.isNotEmpty() && eventSequence.isNotEmpty()) {
            throw IllegalArgumentException(
                "Sequence shot $name only supports switch_sequence or event_sequence"
            )
        }

        // Build sequence events list from event_sequence
        sequenceEvents.addAll(eventSequence.map { it.toString() })

        // Build sequence events list from switch_sequence
        for (switch in switchSequence) {
            // TODO: Get active event for switch when switch controller is available
            /*
            val switchName = (switch as? Switch)?.name ?: switch.toString()
            val activeEvent = machine.switchController.getActiveEventForSwitch(switchName)
            sequenceEvents.add(activeEvent)
            */
        }
    }

    /**
     * Register event and switch handlers.
     */
    private fun registerHandlers() {
        // Register for sequence events
        for (event in sequenceEvents.toSet()) {
            // TODO: Register event handler when event system is available
            /*
            machine.events.addHandler(event) { eventName ->
                sequenceAdvance(eventName)
            }
            */
        }

        // Register for cancel switches
        val cancelSwitches = config["cancel_switches"] as? List<*> ?: emptyList<Any>()
        for (switchObj in cancelSwitches) {
            // TODO: Register switch handler when switch controller is available
            /*
            val switch = switchObj as? Switch
            machine.switchController.addSwitchHandlerObj(switch, ::eventCancel, 1)
            */
        }

        // Register for delay switches
        val delaySwitchList = config["delay_switch_list"] as? Map<*, *> ?: emptyMap<Any, Any>()
        for ((switchObj, ms) in delaySwitchList) {
            // TODO: Register switch handler when switch controller is available
            /*
            val switch = switchObj as? Switch
            val switchName = switch?.name ?: switchObj.toString()
            val delayMs = (ms as? Number)?.toInt() ?: 0

            machine.switchController.addSwitchHandlerObj(
                switch,
                { delaySwitchHit(switchName, delayMs) },
                1
            )
            */
        }

        // Register for delay events
        val delayEventList = config["delay_event_list"] as? Map<*, *> ?: emptyMap<Any, Any>()
        for ((event, ms) in delayEventList) {
            val eventName = event.toString()
            val delayMs = (ms as? Number)?.toInt() ?: 0

            // TODO: Register event handler when event system is available
            /*
            machine.events.addHandler(eventName) {
                delaySwitchHit(eventName, delayMs)
            }
            */
        }
    }

    /**
     * Remove event and switch handlers.
     */
    private fun removeHandlers() {
        // TODO: Remove event handlers when event system is available
        /*
        machine.events.removeHandler(::sequenceAdvance)
        machine.events.removeHandler(::delaySwitchHit)
        */

        val cancelSwitches = config["cancel_switches"] as? List<*> ?: emptyList<Any>()
        for (switchObj in cancelSwitches) {
            // TODO: Remove switch handler when switch controller is available
            /*
            val switchName = (switchObj as? Switch)?.name ?: switchObj.toString()
            machine.switchController.removeSwitchHandler(switchName, ::eventCancel, 1)
            */
        }

        val delaySwitchList = config["delay_switch_list"] as? Map<*, *> ?: emptyMap<Any, Any>()
        for (switchObj in delaySwitchList.keys) {
            // TODO: Remove switch handler when switch controller is available
            /*
            val switchName = (switchObj as? Switch)?.name ?: switchObj.toString()
            machine.switchController.removeSwitchHandler(switchName, ::delaySwitchHit, 1)
            */
        }
    }

    /**
     * Sequence advance handler.
     *
     * @param eventName Event name that triggered the advance
     */
    private fun sequenceAdvance(eventName: String) {
        // Mark playfield active if a playfield is defined for this sequence
        val playfield = config["playfield"]
        // TODO: Mark playfield active when available
        /*
        playfield?.markPlayfieldActiveFromDeviceAction(name)
        */

        debugLog("Sequence advance: $eventName")

        if (eventName == sequenceEvents.firstOrNull()) {
            if (sequenceEvents.size > 1) {
                // Start a new sequence
                startNewSequence()
            } else if (activeDelays.isEmpty()) {
                // If it only has one step it will finish right away
                completed()
            }
        } else {
            // Get the seq_id of the first sequence this event is next for
            // This is not a loop because we only want to advance 1 sequence
            val seq = activeSequences.firstOrNull { it.nextEvent == eventName }

            if (seq != null) {
                // Advance this sequence
                advanceSequence(seq)
            }
        }
    }

    /**
     * Start a new sequence.
     */
    private fun startNewSequence() {
        // If the sequence hasn't started, make sure we're not within the
        // delay_switch hit window
        if (activeDelays.isNotEmpty()) {
            debugLog("There's a delay timer in effect from $activeDelays. Sequence will not be started.")
            return
        }

        // Record start time
        startTime = machine.clock.getTime()

        // Create a new sequence
        val seqId = UUID.randomUUID()
        val nextEvent = sequenceEvents.getOrNull(1) ?: ""

        debugLog("Setting up a new sequence. Next: $nextEvent")

        activeSequences.add(ActiveSequence(seqId, 0, nextEvent))

        // If this sequence has a time limit, set that up
        val sequenceTimeout = (config["sequence_timeout"] as? Number)?.toLong()
        if (sequenceTimeout != null) {
            debugLog("Setting up a sequence timer for ${sequenceTimeout}ms")

            delay.reset(seqId.toString(), sequenceTimeout) {
                sequenceTimeout(seqId)
            }
        }
    }

    /**
     * Advance a sequence.
     *
     * @param sequence Sequence to advance
     */
    private fun advanceSequence(sequence: ActiveSequence) {
        // Remove this sequence from the list
        activeSequences.remove(sequence)

        if (sequence.currentPositionIndex == (sequenceEvents.size - 2)) {
            // Complete!
            debugLog("Sequence complete!")

            delay.remove(sequence.id.toString())
            completed()
        } else {
            val currentPositionIndex = sequence.currentPositionIndex + 1
            val nextEvent = sequenceEvents.getOrNull(currentPositionIndex + 1) ?: ""

            debugLog("Advancing the sequence. Next: $nextEvent")

            activeSequences.add(
                ActiveSequence(sequence.id, currentPositionIndex, nextEvent)
            )
        }
    }

    /**
     * Sequence completed - post completion event.
     */
    private fun completed() {
        // Measure the elapsed time between start and completion of the sequence
        val elapsed = if (startTime != null) {
            machine.clock.getTime() - startTime!!
        } else {
            0.0
        }

        machine.events.post("${name}_hit", mapOf("elapsed" to elapsed))
    }

    /**
     * Event handler for cancel event.
     *
     * TODO: Add @EventHandler(0) annotation when event system is fully integrated
     */
    fun eventCancel() {
        resetAllSequences()
    }

    /**
     * Reset all sequences.
     */
    fun resetAllSequences() {
        val seqIds = activeSequences.map { it.id.toString() }

        for (seqId in seqIds) {
            delay.remove(seqId)
        }

        activeSequences.clear()
    }

    /**
     * Delay switch/event hit handler.
     *
     * @param name Name of the switch/event
     * @param ms Delay time in milliseconds
     */
    private fun delaySwitchHit(name: String, ms: Int) {
        debugLog("Delaying sequence by ${ms}ms")

        delay.reset("${name}_delay_timer", ms.toLong()) {
            releaseDelay(name)
        }

        activeDelays.add(name)
    }

    /**
     * Release a delay.
     *
     * @param delayName Name of the delay to release
     */
    private fun releaseDelay(delayName: String) {
        activeDelays.remove(delayName)
    }

    /**
     * Sequence timeout handler.
     *
     * @param seqId UUID of the sequence that timed out
     */
    private fun sequenceTimeout(seqId: UUID) {
        debugLog("Sequence $seqId timed out")

        activeSequences.removeAll { it.id == seqId }

        machine.events.post("${name}_timeout")
    }
}

/**
 * Event: (name)_hit
 *
 * The sequence_shot called (name) was just completed.
 *
 * Args:
 * - elapsed: Time in seconds to complete the sequence
 */
