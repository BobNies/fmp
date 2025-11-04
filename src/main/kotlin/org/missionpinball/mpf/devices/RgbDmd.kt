package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A physical RGB DMD (Dot Matrix Display).
 *
 * Represents a color (RGB) dot matrix display in the pinball machine.
 * Can receive frame updates via BCP (Bridge Control Protocol) or
 * direct API calls. Supports brightness control.
 */
class RgbDmd(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "rgb_dmds"
    override val collection = "rgb_dmds"
    override val classLabel = "rgb_dmd"

    /**
     * Hardware device interface.
     */
    var hwDevice: Any? = null

    /**
     * Platform for RGB DMD.
     */
    var platform: Any? = null

    companion object {
        /**
         * Device class initialization.
         *
         * Registers BCP command callbacks for receiving RGB DMD frames.
         *
         * @param machine Machine controller
         */
        fun deviceClassInit(machine: MachineController) {
            // TODO: Register BCP command callback when BCP is implemented
            /*
            machine.bcp.interface.registerCommandCallback("rgb_dmd_frame") { client, params ->
                bcpReceiveDmdFrame(machine, client, params)
            }
            */
        }

        /**
         * Receive RGB DMD frame from BCP.
         *
         * @param machine Machine controller
         * @param client BCP client
         * @param name RGB DMD name
         * @param rawbytes Frame data
         */
        suspend fun bcpReceiveDmdFrame(
            machine: MachineController,
            client: Any,
            name: String,
            rawbytes: ByteArray
        ) {
            // TODO: Implement when RGB DMD collection is available
            /*
            val rgbDmd = machine.rgbDmds[name]
                ?: throw IllegalArgumentException("rgb dmd $name not known")

            rgbDmd.update(rawbytes)
            */
        }
    }

    override suspend fun initialize() {
        super.initialize()

        // TODO: Configure platform when platform system is complete
        /*
        platform = machine.getPlatformSections("rgb_dmd", config["platform"])
        (platform as RgbDmdPlatform).assertHasFeature("rgb_dmds")
        hwDevice = platform.configureRgbDmd(name)
        updateBrightness(null)
        */

        logger.warn { "RGB DMD platform not yet fully implemented for $name" }
    }

    /**
     * Update brightness from config.
     *
     * @param future Brightness change future (for callback)
     */
    private fun updateBrightness(future: Any?) {
        // TODO: Implement when placeholder manager is available
        /*
        val (brightness, brightnessChangedFuture) = config["hardware_brightness"]
            .evaluateAndSubscribe(emptyList())

        hwDevice?.setBrightness(brightness)
        brightnessChangedFuture.addDoneCallback(::updateBrightness)
        */
    }

    /**
     * Update data on the RGB DMD.
     *
     * @param data Bytes to send to the display
     */
    fun update(data: ByteArray) {
        // TODO: Implement when hardware device is available
        /*
        hwDevice?.update(data)
        */
    }
}
