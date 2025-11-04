package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * A physical DMD (Dot Matrix Display).
 *
 * Represents a monochrome dot matrix display in the pinball machine.
 * Can receive frame updates via BCP (Bridge Control Protocol) or
 * direct API calls.
 */
class Dmd(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "dmds"
    override val collection = "dmds"
    override val classLabel = "dmd"

    /**
     * Hardware device interface.
     */
    var hwDevice: Any? = null

    /**
     * Platform for DMD.
     */
    var platform: Any? = null

    companion object {
        /**
         * Device class initialization.
         *
         * Registers BCP command callbacks for receiving DMD frames.
         *
         * @param machine Machine controller
         */
        fun deviceClassInit(machine: MachineController) {
            // TODO: Register BCP command callback when BCP is implemented
            /*
            machine.bcp.interface.registerCommandCallback("dmd_frame") { client, params ->
                bcpReceiveDmdFrame(machine, client, params)
            }
            */
        }

        /**
         * Receive DMD frame from BCP.
         *
         * @param machine Machine controller
         * @param client BCP client
         * @param name DMD name
         * @param rawbytes Frame data
         */
        suspend fun bcpReceiveDmdFrame(
            machine: MachineController,
            client: Any,
            name: String,
            rawbytes: ByteArray
        ) {
            // TODO: Implement when DMD collection is available
            /*
            val dmd = machine.dmds[name]
                ?: throw IllegalArgumentException("dmd $name not known")

            dmd.update(rawbytes)
            */
        }
    }

    override suspend fun initialize() {
        super.initialize()

        // TODO: Configure platform when platform system is complete
        /*
        platform = machine.getPlatformSections("dmd", config["platform"])
        (platform as DmdPlatform).assertHasFeature("dmds")
        hwDevice = platform.configureDmd()
        */

        logger.warn { "DMD platform not yet fully implemented for $name" }
    }

    /**
     * Update data on the DMD.
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
