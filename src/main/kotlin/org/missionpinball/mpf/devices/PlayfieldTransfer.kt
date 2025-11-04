package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Transfer a ball between two playfields.
 *
 * E.g. lower to upper playfield via a ramp.
 *
 * Device which moves a ball from one playfield to another. This tracks the ball
 * leaving the source playfield and arriving on the target playfield, posting
 * appropriate events to maintain ball counts.
 */
class PlayfieldTransfer(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "playfield_transfers"
    override val collection = "playfield_transfers"
    override val classLabel = "playfield_transfer"

    /**
     * Target playfield where balls are transferred to.
     */
    var target: Any? = null

    /**
     * Source playfield where balls are captured from.
     */
    var source: Any? = null

    override suspend fun initialize() {
        super.initialize()

        if (config["ball_switch"] != null) {
            // TODO: Register switch handler when init phases are available
            /*
            machine.events.addHandler("init_phase_3", ::configureSwitch)
            */
        }

        // Load target playfield
        target = config["eject_target"]
        source = config["captures_from"]
    }

    /**
     * Configure switch handler.
     */
    private fun configureSwitch() {
        val ballSwitch = config["ball_switch"] as? Switch

        // TODO: Register switch handler when switch controller is available
        /*
        machine.switchController.addSwitchHandlerObj(
            switch = ballSwitch,
            callback = ::transfer,
            state = 1,
            ms = 0
        )
        */
    }

    /**
     * Event handler for transfer event.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventTransfer() {
        transfer()
    }

    /**
     * Transfer a ball to the target playfield.
     *
     * This method posts a series of events to properly track the ball leaving
     * the source playfield and arriving on the target playfield.
     */
    fun transfer() {
        val sourceName = (source as? Any)?.toString() ?: "unknown"
        val targetName = (target as? Any)?.toString() ?: "unknown"

        debugLog("Ball went from $sourceName to $targetName")

        // Source playfield is obviously active
        // We will continue using a callback to keep the ball count sane
        // (otherwise it may go to -1 during the next event)
        machine.events.post(
            "sw_${sourceName}_active",
            mapOf("balls" to 1),
            callback = { ballWentThrough2() }
        )

        machine.events.post(
            "playfield_transfer_${name}_ball_transferred",
            mapOf("source" to source, "target" to target)
        )
    }

    /**
     * Used as callback in transfer (step 2).
     *
     * Trigger remove ball from source playfield.
     */
    private fun ballWentThrough2() {
        val sourceName = (source as? Any)?.toString() ?: "unknown"

        machine.events.post(
            "balldevice_captured_from_$sourceName",
            mapOf("balls" to 1)
        )

        // Inform target playfield about incoming ball
        machine.events.post(
            "balldevice_${name}_ejecting_ball",
            mapOf(
                "balls" to 1,
                "target" to target,
                "timeout" to 0
            ),
            callback = { ballWentThrough3() }
        )
    }

    /**
     * Used as callback in ballWentThrough2 (step 3).
     *
     * Promise (and hope) that it actually goes to the target.
     */
    private fun ballWentThrough3() {
        machine.events.post(
            "balldevice_${name}_ball_eject_success",
            mapOf(
                "balls" to 1,
                "target" to target
            ),
            callback = { ballWentThrough4() }
        )

        // TODO: Update target available balls when playfield is fully available
        /*
        (target as? Playfield)?.availableBalls = ((target as? Playfield)?.availableBalls ?: 0) + 1
        */
    }

    /**
     * Used as callback in ballWentThrough3 (step 4).
     *
     * Since we confirmed eject, target playfield has to be active.
     */
    private fun ballWentThrough4() {
        val targetName = (target as? Any)?.toString() ?: "unknown"

        machine.events.post("sw_${targetName}_active")
    }
}

/**
 * Event: playfield_transfer_(playfield_transfer)_ball_transferred
 *
 * The playfield_transfer called (playfield_transfer) transferred a ball from
 * playfield (source) to playfield (target).
 *
 * Args:
 * - source: The source playfield
 * - target: The target playfield
 */
