package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A shot profile.
 *
 * Shot profiles define the behavior and settings for shots (targets, lanes, etc.)
 * in the game. They can be defined system-wide or within modes.
 *
 * Note: In Python, this class uses multiple inheritance from both ModeDevice
 * and SystemWideDevice. In Kotlin, we extend SystemWideDevice and add
 * mode-specific functionality directly.
 */
class ShotProfile(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "shot_profiles"
    override val collection = "shot_profiles"
    override val classLabel = "shot_profile"

    /**
     * Mode this profile belongs to (if any).
     */
    var mode: Mode? = null

    /**
     * Remove profile from mode.
     *
     * @param mode Mode to remove from
     */
    fun deviceRemovedFromMode(mode: Mode) {
        this.mode = null
    }
}
