package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.DelayManager
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.Player
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Represents a single drop target in a pinball machine.
 *
 * Drop targets are targets that "drop" down when hit by the ball.
 * They can be reset (raised back up) by a reset coil, and optionally
 * knocked down by a knockdown coil.
 *
 * Note: In Python, this uses @DeviceMonitor("complete") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class DropTarget(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "drop_targets"
    override val collection = "drop_targets"
    override val classLabel = "drop_target"

    /**
     * Reset coil to raise the target back up.
     */
    var resetCoil: Driver? = null

    /**
     * Knockdown coil to force the target down.
     */
    var knockdownCoil: Driver? = null

    /**
     * Banks this target belongs to.
     */
    val banks = mutableSetOf<DropTargetBank>()

    /**
     * Whether the target is down (complete).
     */
    var complete = false

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Playfield this drop target is on.
     */
    var playfield: Any? = null

    override suspend fun initialize() {
        super.initialize()

        resetCoil = config["reset_coil"] as? Driver
        knockdownCoil = config["knockdown_coil"] as? Driver

        // TODO: Register switch handlers when switch controller is available
        /*
        // Can't read the switch until the switch controller is set up
        machine.events.addHandler("init_phase_4", ::updateStateFromSwitch, priority = 2, reconcile = true)
        machine.events.addHandler("init_phase_4", ::registerSwitchHandlers, priority = 1)
        */

        // If a playfield is not explicitly defined, defer to the switch's playfield
        val switch = config["switch"] as? Switch
        playfield = config["playfield"] ?: switch?.playfield

        val ballSearchOrder = config["ball_search_order"] as? Int
        if (ballSearchOrder != null) {
            // TODO: Register ball search when available
            /*
            playfield?.ballSearch?.register(ballSearchOrder, ::ballSearch, name)
            */
        }

        // Validate switch doesn't have redundant playfield active tag
        // TODO: Check when switch tags are available
        /*
        val playfieldActiveName = "${playfield?.name}_active"
        if (switch?.tags?.contains(playfieldActiveName) == true) {
            raiseConfigError(
                "Drop target device '$name' uses switch '${switch.name}' which has a " +
                "'${playfieldActiveName}' tag. This is handled internally by the device. Remove the " +
                "redundant '${playfieldActiveName}' tag from that switch.", 1
            )
        }
        */
    }

    /**
     * Ignore switch hits for a specified duration.
     *
     * @param ms Milliseconds to ignore hits
     * @param resetAttempt Optional reset attempt counter
     */
    private fun ignoreSwitchHitsFor(ms: Long, resetAttempt: Int? = null) {
        debugLog("Ignoring switch hits for ${ms}ms")

        val switch = config["switch"] as? Switch
        // TODO: Implement when Switch.mute is available
        // switch?.mute(this)

        delay.reset("ignore_switch", ms) {
            restoreSwitchHits(resetAttempt)
        }
    }

    /**
     * Restore switch hit monitoring.
     *
     * @param resetAttempt Optional reset attempt counter
     */
    private fun restoreSwitchHits(resetAttempt: Int? = null) {
        debugLog("Restoring switch hits")

        val switch = config["switch"] as? Switch
        // TODO: Implement when Switch.unmute is available
        // switch?.unmute(this)

        updateStateFromSwitch(reconcile = true)

        if (complete && resetAttempt != null) {
            val maxResetAttempts = config["max_reset_attempts"] as? Int
            if (maxResetAttempts != null && resetAttempt < maxResetAttempts) {
                debugLog("Reset failed after attempt $resetAttempt, trying again")
                reset(resetAttempt + 1)
            } else if (maxResetAttempts != null) {
                infoLog("Failed to reset after $resetAttempt attempts. Giving up.")
            }
        } else if (resetAttempt != null) {
            debugLog("Reset confirmed!")
        }
    }

    /**
     * Ball search phase 1: Reset if up, knockdown if down.
     */
    private fun ballSearchPhase1(): Boolean {
        if (!complete && resetCoil != null) {
            ballSearchReset()
            return true
        }

        if (complete && knockdownCoil != null) {
            ballSearchKnockdown()
            return true
        }

        return false
    }

    /**
     * Ball search phase 2: Reset and knockdown if both available.
     */
    private fun ballSearchPhase2(): Boolean {
        if (resetCoil != null && knockdownCoil != null) {
            if (complete) {
                ballSearchReset()
                delay.add(100L, "ball_search_knockdown") {
                    ballSearchKnockdown()
                }
            } else {
                ballSearchKnockdown()
                delay.add(100L, "ball_search_reset") {
                    ballSearchReset()
                }
            }
            return true
        }

        // Fall back to phase 1
        return ballSearchPhase1()
    }

    /**
     * Ball search phase 3: More aggressive search.
     */
    private fun ballSearchPhase3(): Boolean {
        if (complete) {
            if (resetCoil != null) {
                ballSearchReset()
                if (knockdownCoil != null) {
                    delay.add(100L, "ball_search_knockdown") {
                        ballSearchKnockdown()
                    }
                }
                return true
            }
            return ballSearchPhase1()
        }

        if (knockdownCoil != null) {
            ballSearchKnockdown()
            if (resetCoil != null) {
                delay.add(100L, "ball_search_reset") {
                    ballSearchReset()
                }
            }
            return true
        }

        return ballSearchPhase1()
    }

    /**
     * Knockdown target during ball search.
     */
    private fun ballSearchKnockdown() {
        val ignoreMs = (config["ignore_switch_ms"] as? Number)?.toLong() ?: 300L
        ignoreSwitchHitsFor(ignoreMs)
        knockdownCoil?.pulse()
    }

    /**
     * Reset target during ball search.
     */
    private fun ballSearchReset() {
        val ignoreMs = (config["ignore_switch_ms"] as? Number)?.toLong() ?: 300L
        ignoreSwitchHitsFor(ignoreMs)
        resetCoil?.pulse()
    }

    /**
     * Ball search callback.
     *
     * @param phase Ball search phase (1-3)
     * @param iteration Ball search iteration
     * @return True if this device can help with ball search
     */
    private fun ballSearch(phase: Int, iteration: Int): Boolean {
        return when (phase) {
            1 -> ballSearchPhase1()
            2 -> ballSearchPhase2()
            else -> ballSearchPhase3()
        }
    }

    /**
     * Register switch handlers.
     */
    private fun registerSwitchHandlers() {
        val switch = config["switch"] as? Switch
        // TODO: Register switch handlers when switch controller is available
        /*
        machine.switchController.addSwitchHandlerObj(switch, ::updateStateFromSwitch, 0)
        machine.switchController.addSwitchHandlerObj(switch, ::updateStateFromSwitch, 1)
        */
    }

    /**
     * Event handler for enable_keep_up event.
     *
     * TODO: Add @EventHandler(6) annotation when event system is fully integrated
     */
    fun eventEnableKeepUp() {
        enableKeepUp()
    }

    /**
     * Keep the target up by enabling the coil.
     */
    fun enableKeepUp() {
        resetCoil?.enable()
    }

    /**
     * Event handler for disable_keep_up event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventDisableKeepUp() {
        disableKeepUp()
    }

    /**
     * No longer keep the target up.
     */
    fun disableKeepUp() {
        resetCoil?.disable()
    }

    /**
     * Event handler for knockdown event.
     *
     * TODO: Add @EventHandler(7) annotation when event system is fully integrated
     */
    fun eventKnockdown() {
        knockdown()
    }

    /**
     * Pulse the knockdown coil to knock down this drop target.
     */
    fun knockdown() {
        val switch = config["switch"] as? Switch
        val isActive = false // TODO: machine.switchController.isActive(switch) when available

        if (knockdownCoil != null && !isActive) {
            val ignoreMs = (config["ignore_switch_ms"] as? Number)?.toLong() ?: 300L
            ignoreSwitchHitsFor(ignoreMs)

            val maxWaitMs = config["knockdown_coil_max_wait_ms"] as? Int
            knockdownCoil?.pulse(maxWaitMs = maxWaitMs)
        }
    }

    /**
     * Update state from switch.
     *
     * @param reconcile True if this is a reconciliation (don't mark playfield active)
     */
    private fun updateStateFromSwitch(reconcile: Boolean = false) {
        val switch = config["switch"] as? Switch
        val isComplete = false // TODO: machine.switchController.isActive(switch) when available

        debugLog("Drop target $name switch ${switch?.name} has active value $isComplete compared to drop complete $complete")

        if (!reconcile) {
            // TODO: Mark playfield active when available
            // playfield?.markPlayfieldActiveFromDeviceAction(name)
        }

        if (isComplete != complete) {
            if (isComplete) {
                down()
            } else {
                up()
            }

            updateBanks(reconcile)
        }
    }

    /**
     * Handle target going down.
     */
    private fun down() {
        complete = true
        machine.events.post("drop_target_${name}_down", mapOf("device" to this))
    }

    /**
     * Handle target going up.
     */
    private fun up() {
        complete = false
        machine.events.post("drop_target_${name}_up", mapOf("device" to this))
    }

    /**
     * Update all banks this target belongs to.
     *
     * @param reconcile True if this is a reconciliation
     */
    private fun updateBanks(reconcile: Boolean = false) {
        for (bank in banks) {
            bank.memberTargetChange(reconcile)
        }
    }

    /**
     * Add this drop target to a drop target bank.
     *
     * This allows the bank to update its status based on state changes to this drop target.
     *
     * @param bank DropTargetBank object to add this drop target to
     */
    fun addToBank(bank: DropTargetBank) {
        banks.add(bank)
    }

    /**
     * Handle the reset from our bank.
     *
     * The bank might pulse the coil from this device or it might have a
     * separate reset coil which will trigger a reset on switch of this device.
     * Make sure we do not mark the playfield as active.
     */
    fun externalResetFromBank() {
        updateStateFromSwitch(reconcile = true)
    }

    /**
     * Remove the DropTarget from a bank.
     *
     * @param bank DropTargetBank object to remove
     */
    fun removeFromBank(bank: DropTargetBank) {
        banks.remove(bank)
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
     * Reset this drop target.
     *
     * If this drop target is configured with a reset coil, then this method
     * will pulse that coil. If not, then it checks to see if this drop target
     * is part of a drop target bank, and if so, it calls the reset() method of
     * the drop target bank.
     *
     * This method does not reset the target profile, however, the switch event
     * handler should reset the target profile on its own when the drop target
     * physically moves back to the up position.
     *
     * @param attempt Reset attempt number (for retry logic)
     */
    fun reset(attempt: Int? = null) {
        if (resetCoil != null && complete) {
            var resetAttempt = attempt
            val maxResetAttempts = config["max_reset_attempts"] as? Int
            if (maxResetAttempts != null && resetAttempt == null) {
                resetAttempt = 1
            }

            val ignoreMs = (config["ignore_switch_ms"] as? Number)?.toLong() ?: 300L
            ignoreSwitchHitsFor(ignoreMs, resetAttempt)

            val maxWaitMs = config["reset_coil_max_wait_ms"] as? Int
            resetCoil?.pulse(maxWaitMs = maxWaitMs)
        }
    }
}

/**
 * States of the drop target bank.
 */
enum class DropTargetBankState {
    /** Initial/unknown state */
    UNKNOWN,
    /** All targets are up */
    UP,
    /** All targets are down */
    DOWN,
    /** Some targets up, some down */
    MIXED
}

/**
 * A bank of drop targets in a pinball machine by grouping together multiple DropTarget class devices.
 *
 * Drop target banks track the collective state of multiple drop targets and can
 * reset all targets together. They post events when all targets are down, all up,
 * or in a mixed state.
 *
 * Note: In Python, this uses @DeviceMonitor("complete", "down", "up", "state") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this inherits from both SystemWideDevice and ModeDevice.
 * In Kotlin, it extends SystemWideDevice and includes mode-related functionality directly.
 */
class DropTargetBank(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "drop_target_banks"
    override val collection = "drop_target_banks"
    override val classLabel = "drop_target_bank"

    /**
     * Drop targets in this bank.
     */
    val dropTargets = mutableListOf<DropTarget>()

    /**
     * Single reset coil for the entire bank.
     */
    var resetCoil: Driver? = null

    /**
     * Multiple reset coils for the bank.
     */
    val resetCoils = mutableSetOf<Driver>()

    /**
     * Whether all targets in the bank are down.
     */
    var complete = false

    /**
     * Current state of the bank.
     */
    var state = DropTargetBankState.UNKNOWN

    /**
     * Number of targets currently down.
     */
    var down = 0

    /**
     * Number of targets currently up.
     */
    var up = 0

    /**
     * Delay manager for timed operations.
     */
    private val delay = DelayManager(machine.clock)

    /**
     * Mode this device is associated with (if in mode).
     */
    var mode: Mode? = null

    /**
     * Whether this device can exist outside of a game.
     */
    val canExistOutsideOfGame: Boolean = true

    override suspend fun initialize() {
        super.initialize()

        @Suppress("UNCHECKED_CAST")
        (config["drop_targets"] as? List<*>)?.let { targets ->
            dropTargets.addAll(targets.filterIsInstance<DropTarget>())
        }

        resetCoil = config["reset_coil"] as? Driver

        @Suppress("UNCHECKED_CAST")
        (config["reset_coils"] as? Set<*>)?.let { coils ->
            resetCoils.addAll(coils.filterIsInstance<Driver>())
        }

        // If individual drop targets have reset coils, they will ball search themselves.
        // The bank will only trigger in ball search if it has its own bank coils defined
        val ballSearchOrder = config["ball_search_order"] as? Int
        if (ballSearchOrder != null && (resetCoil != null || resetCoils.isNotEmpty())) {
            // TODO: Register ball search when available
            /*
            val playfield = config["playfield"]
            playfield?.ballSearch?.register(ballSearchOrder, ::ballSearch, name)
            */
        }
    }

    /**
     * Called when device is loaded in a mode.
     */
    fun deviceLoadedInMode(mode: Mode, player: Player) {
        addTargetsToBank()
    }

    /**
     * Called when device is added system-wide.
     */
    suspend fun deviceAddedSystemWide() {
        addTargetsToBank()
    }

    /**
     * Add targets to bank.
     */
    private fun addTargetsToBank() {
        for (target in dropTargets) {
            target.addToBank(this)

            val bankPlayfield = config["playfield"]
            require(bankPlayfield == target.playfield) {
                "Drop target bank has a playfield $bankPlayfield but target ${target.name} " +
                "has playfield ${target.playfield}. Banks do not support targets on multiple playfields."
            }
        }

        memberTargetChange()
        debugLog("Drop Targets: $dropTargets")
    }

    /**
     * Event handler for reset event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventReset() {
        reset()
    }

    /**
     * Reset this bank of drop targets.
     *
     * This method has some intelligence to figure out what coil(s) it should
     * fire. It builds up a set by looking at its own reset_coil and
     * reset_coils settings, and also scanning through all the member drop
     * targets and collecting their coils. Then it pulses each of them. (This
     * coil list is a "set" which means it only sends a single pulse to each
     * coil, even if each drop target is configured with its own coil.)
     *
     * @param attempt Reset attempt number (for retry logic)
     */
    fun reset(attempt: Int? = null) {
        debugLog("Resetting")

        if (down == 0) {
            infoLog("All targets are already up. Will not reset bank.")
            return
        }

        infoLog("$down targets are down. Will reset those.")

        // Figure out all the coils we need to pulse
        val coils = mutableSetOf<Driver>()

        for (dropTarget in dropTargets) {
            // Add all reset coils for targets which are down
            if (dropTarget.resetCoil != null && dropTarget.complete) {
                coils.add(dropTarget.resetCoil!!)
            }

            // Mute all the bank's switches
            val ignoreSwitchMs = config["ignore_switch_ms"] as? Long
            if (ignoreSwitchMs != null && ignoreSwitchMs > 0) {
                // TODO: Implement when Switch.mute is available
                // dropTarget.config["switch"]?.mute(this)
            }
        }

        for (coil in resetCoils) {
            coils.add(coil)
        }

        if (resetCoil != null) {
            coils.add(resetCoil!!)
        }

        // Now pulse the coils
        debugLog("Pulsing reset coils: $coils")
        var restoreDelayMs = 0L

        for (coil in coils) {
            val maxWaitMs = config["reset_coil_max_wait_ms"] as? Int
            val waitMs = coil.pulse(maxWaitMs = maxWaitMs)
            debugLog("Coil $coil firing has a wait of $waitMs!")
            restoreDelayMs += waitMs.toLong()
        }

        // Set a delay to unmute all the switches after the combined reset time plus ignore
        val ignoreSwitchMs = config["ignore_switch_ms"] as? Long
        if (ignoreSwitchMs != null && ignoreSwitchMs > 0) {
            debugLog("Switches ignored, setting ${ignoreSwitchMs}ms timer to restore them.")
            restoreDelayMs += ignoreSwitchMs

            var resetAttempt = attempt
            val maxResetAttempts = config["max_reset_attempts"] as? Int
            if (maxResetAttempts != null && resetAttempt == null) {
                resetAttempt = 1
            }

            delay.add(restoreDelayMs, "ignore_hits") {
                restoreSwitchHits(resetAttempt)
            }
        }
    }

    /**
     * Restore switch hits after reset.
     *
     * @param resetAttempt Reset attempt number (for retry logic)
     */
    private fun restoreSwitchHits(resetAttempt: Int? = null) {
        debugLog("Restoring switch hits")

        for (target in dropTargets) {
            // TODO: Implement when Switch.unmute is available
            // target.config["switch"]?.unmute(this)
            target.externalResetFromBank()
        }

        memberTargetChange()

        val maxResetAttempts = config["max_reset_attempts"] as? Int
        if (down != 0 && resetAttempt != null && maxResetAttempts != null) {
            if (resetAttempt < maxResetAttempts) {
                debugLog("Reset failed after attempt $resetAttempt, trying again.")
                reset(resetAttempt + 1)
            } else {
                infoLog("Failed to reset after $resetAttempt attempts. Giving up.")
            }
        } else {
            debugLog("Reset confirmed!")
        }
    }

    /**
     * Ball search callback.
     *
     * @param phase Ball search phase
     * @param iteration Ball search iteration
     */
    private fun ballSearch(phase: Int, iteration: Int) {
        val attempt = (iteration - 1) * phase + iteration
        reset(attempt)
    }

    /**
     * Handle that a member drop target has changed state.
     *
     * This method causes this group to update its down and up counts and
     * complete status.
     *
     * @param reconcile True if this is a reconciliation (don't post events)
     */
    fun memberTargetChange(reconcile: Boolean = false) {
        down = 0
        up = 0

        for (target in dropTargets) {
            if (target.complete) {
                down++
            } else {
                up++
            }
        }

        debugLog(
            "Member drop target status change: Up: $up, Down: $down, " +
            "Total: ${dropTargets.size}, Reconcile: $reconcile"
        )

        // Don't change the internal state during reconciliation. After the reset
        // is complete the bank will re-check and post the final state.
        if (reconcile) {
            return
        }

        when {
            down == dropTargets.size -> bankDown()
            down == 0 -> bankUp()
            else -> bankMixed()
        }
    }

    /**
     * Handle all targets down.
     */
    private fun bankDown() {
        if (state == DropTargetBankState.DOWN) {
            return
        }

        state = DropTargetBankState.DOWN
        complete = true
        debugLog("All targets are down")

        val resetOnComplete = (config["reset_on_complete"] as? Number)?.toLong()
        if (resetOnComplete != null) {
            debugLog("Reset on complete after $resetOnComplete")
            delay.add(resetOnComplete, "reset_on_complete") {
                reset()
            }
        }

        machine.events.post("drop_target_bank_${name}_down")
    }

    /**
     * Handle all targets up.
     */
    private fun bankUp() {
        if (state == DropTargetBankState.UP) {
            return
        }

        state = DropTargetBankState.UP
        complete = false
        debugLog("All targets are up")

        machine.events.post("drop_target_bank_${name}_up")
    }

    /**
     * Handle mixed state.
     */
    private fun bankMixed() {
        val prevState = state
        state = DropTargetBankState.MIXED
        complete = false

        machine.events.post(
            "drop_target_bank_${name}_mixed",
            mapOf("prev_value" to prevState, "down" to down)
        )
    }

    /**
     * Called when device is removed from a mode.
     */
    fun deviceRemovedFromMode(mode: Mode) {
        for (target in dropTargets) {
            target.removeFromBank(this)
        }
    }
}

/**
 * Event: drop_target_(name)_down
 *
 * The drop target with the (name) has just changed to the "down" state.
 */

/**
 * Event: drop_target_(name)_up
 *
 * The drop target (name) has just changed to the "up" state.
 */

/**
 * Event: drop_target_bank_(name)_down
 *
 * Every drop target in the drop target bank called (name) is now in the "down" state.
 * This event is only posted once, when all the drop targets are down.
 */

/**
 * Event: drop_target_bank_(name)_up
 *
 * Every drop target in the drop target bank called (name) is now in the "up" state.
 * This event is only posted once, when all the drop targets are up.
 */

/**
 * Event: drop_target_bank_(name)_mixed
 *
 * The drop targets in the drop target bank (name) are in a "mixed" state,
 * meaning that they're not all down or not all up. This event is posted every
 * time a member drop target changes but the overall bank is not complete.
 */
