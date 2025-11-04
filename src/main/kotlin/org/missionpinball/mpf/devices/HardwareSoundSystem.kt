package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Hardware sound system used in EM and SS machines.
 *
 * This device provides an interface to hardware-based sound systems,
 * typically found in electromechanical (EM) and solid state (SS) pinball
 * machines. It supports playing sound numbers, files, and text-to-speech,
 * as well as volume control per track.
 */
class HardwareSoundSystem(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "hardware_sound_systems"
    override val collection = "hardware_sound_systems"
    override val classLabel = "hardware_sound_system"

    /**
     * Hardware sound platform interface.
     */
    var hwDevice: Any? = null

    /**
     * Platform for hardware sound system.
     */
    var platform: Any? = null

    /**
     * Volume levels per track (defaults to 1.0).
     */
    private val volume = mutableMapOf<Int, Double>().withDefault { 1.0 }

    override suspend fun initialize() {
        super.initialize()

        // TODO: Configure platform when platform system is complete
        /*
        platform = machine.getPlatformSections(
            "hardware_sound_system",
            config["platform"]
        )
        (platform as HardwareSoundPlatform).assertHasFeature("hardware_sound_systems")

        hwDevice = platform.configureHardwareSoundSystem(
            config["platform_settings"] as? Map<String, Any?>
        )
        */

        logger.warn { "Hardware sound system platform not yet fully implemented for $name" }
    }

    /**
     * Play a sound by number.
     *
     * @param soundNumber Sound number to play
     * @param track Track number (default: 1)
     */
    fun play(soundNumber: Int, track: Int = 1) {
        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.playSound(soundNumber, track)
        */
        debugLog("Playing sound $soundNumber on track $track")
    }

    /**
     * Play a sound file.
     *
     * @param file Path to sound file
     * @param platformOptions Platform-specific options
     * @param track Track number (default: 1)
     */
    fun playFile(file: String, platformOptions: Map<String, Any?>?, track: Int = 1) {
        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.playSoundFile(file, platformOptions, track)
        */
        debugLog("Playing file $file on track $track")
    }

    /**
     * Text to speech output.
     *
     * @param text Text to speak
     * @param platformOptions Platform-specific options
     * @param track Track number (default: 1)
     */
    fun textToSpeech(text: String, platformOptions: Map<String, Any?>?, track: Int = 1) {
        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.textToSpeech(text, platformOptions, track)
        */
        debugLog("Text to speech: '$text' on track $track")
    }

    /**
     * Set volume for a track.
     *
     * @param volumeLevel Volume level (0.0 to 1.0)
     * @param track Track number (default: 1)
     */
    fun setVolume(volumeLevel: Double, track: Int = 1) {
        volume[track] = volumeLevel

        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.setVolume(volume.getValue(track), track)
        */
        debugLog("Set volume to $volumeLevel on track $track")
    }

    /**
     * Increase volume for a track.
     *
     * @param volumeDelta Amount to increase volume
     * @param track Track number (default: 1)
     */
    fun increaseVolume(volumeDelta: Double, track: Int = 1) {
        val newVolume = volume.getValue(track) + volumeDelta
        volume[track] = newVolume

        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.setVolume(volume.getValue(track), track)
        */
        debugLog("Increased volume by $volumeDelta to $newVolume on track $track")
    }

    /**
     * Decrease volume for a track.
     *
     * @param volumeDelta Amount to decrease volume
     * @param track Track number (default: 1)
     */
    fun decreaseVolume(volumeDelta: Double, track: Int = 1) {
        val newVolume = volume.getValue(track) - volumeDelta
        volume[track] = newVolume

        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.setVolume(volume.getValue(track), track)
        */
        debugLog("Decreased volume by $volumeDelta to $newVolume on track $track")
    }

    /**
     * Stop all sounds on a track.
     *
     * @param track Track number (default: 1)
     */
    fun stopAllSounds(track: Int = 1) {
        // TODO: Implement when hardware interface is available
        /*
        hwDevice?.stopAllSounds(track)
        */
        debugLog("Stopping all sounds on track $track")
    }
}
