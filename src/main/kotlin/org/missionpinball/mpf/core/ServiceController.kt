package org.missionpinball.mpf.core

import org.missionpinball.mpf.devices.Driver
import org.missionpinball.mpf.devices.Switch

/**
 * Switch map entry.
 *
 * @property board Board name
 * @property switch Switch device
 */
data class SwitchMap(
    val board: String,
    val switch: Switch
)

/**
 * Coil map entry.
 *
 * @property board Board name
 * @property coil Driver (coil) device
 */
data class CoilMap(
    val board: String,
    val coil: Driver
)

/**
 * Light map entry.
 *
 * @property board Board name
 * @property light Light device (placeholder - will be Any until Light is converted)
 */
data class LightMap(
    val board: String,
    val light: Any  // TODO: Change to Light when Light device is converted
)

/**
 * Controller for all service functionality.
 *
 * Provides all service information and can perform service tasks.
 * Displaying the information is performed by the service mode or other components.
 */
class ServiceController(machine: MachineController) : MpfController(machine) {

    override val configName = "service_controller"

    /**
     * Whether service mode is currently active.
     */
    private var enabled = false

    init {
        configureLogging("service", null, null, null)
    }

    /**
     * Check if currently in service mode.
     *
     * @return True if in service mode
     */
    fun isInService(): Boolean = enabled

    /**
     * Start service mode.
     *
     * Stops all active modes except service-related modes and posts service_mode_entered event.
     */
    fun startService() {
        check(!isInService()) { "Already in service mode!" }
        enabled = true

        infoLog("Entered service mode. Resetting game if running. Resetting hardware interface now.")

        // TODO: Stop modes when mode system is fully integrated
        /*
        // Stop attract and game mode
        for (mode in machine.modes.values) {
            if (!mode.active || mode.name in listOf("service", "service_segment_display", "service_dmd", "game")) {
                continue
            }
            mode.stop(service = true)
        }

        // Explicitly stop game last
        if (machine.modes["game"]?.active == true) {
            machine.modes["game"]?.stop(service = true)
        }
        */

        machine.events.post("service_mode_entered")
    }

    /**
     * Stop service mode.
     *
     * Posts service_mode_exited event and resets the machine.
     */
    suspend fun stopService() {
        check(isInService()) { "Not in service mode!" }
        enabled = false

        // This event starts attract mode again
        machine.events.post("service_mode_exited")
        // TODO: Call machine reset when implemented
        // machine.reset()
    }

    /**
     * Add an alert about a technical problem.
     *
     * @param device Device with the issue
     * @param issue Description of the issue
     */
    fun addTechnicalAlert(device: Any, issue: String) {
        // TODO: This is prepared but not yet implemented in service mode
    }

    /**
     * Get a map of all switches in the machine.
     *
     * @param doSort Whether to sort the map by board and switch number
     * @return List of switch map entries
     */
    fun getSwitchMap(doSort: Boolean = true): List<SwitchMap> {
        // TODO: Implement when switch collection is available
        /*
        val switchMap = mutableListOf<SwitchMap>()

        for (switch in machine.switches.values) {
            switchMap.add(SwitchMap(
                board = switch.hwSwitch?.getBoardName() ?: "unknown",
                switch = switch
            ))
        }

        if (doSort) {
            switchMap.sortWith(compareBy(
                { naturalKeySort(it.board) },
                { naturalKeySort(it.switch.hwSwitch?.number?.toString() ?: "") }
            ))
        }

        return switchMap
        */
        return emptyList()
    }

    /**
     * Get a map of all coils in the machine.
     *
     * @param doSort Whether to sort the map by board and coil number
     * @return List of coil map entries
     */
    fun getCoilMap(doSort: Boolean = true): List<CoilMap> {
        // TODO: Implement when coil collection is available
        /*
        val coilMap = mutableListOf<CoilMap>()

        for (coil in machine.coils.values) {
            checkNotNull(coil.hwDriver) { "Coil ${coil.name} has no hardware driver" }
            coilMap.add(CoilMap(
                board = coil.hwDriver!!.getBoardName(),
                coil = coil
            ))
        }

        if (doSort) {
            coilMap.sortWith(compareBy(
                { naturalKeySort(it.board) },
                { naturalKeySort(it.coil.hwDriver?.number?.toString() ?: "") }
            ))
        }

        return coilMap
        */
        return emptyList()
    }

    /**
     * Get a map of all lights in the machine.
     *
     * @param doSort Whether to sort the map by board and light number
     * @return List of light map entries
     */
    fun getLightMap(doSort: Boolean = true): List<LightMap> {
        // TODO: Implement when light collection is available
        /*
        val lightMap = mutableListOf<LightMap>()

        for (light in machine.lights.values) {
            val firstDriver = light.hwDrivers.values.first()[0]
            lightMap.add(LightMap(
                board = firstDriver.getBoardName(),
                light = light
            ))
        }

        if (doSort) {
            lightMap.sortWith(compareBy(
                { naturalKeySort(it.board) },
                { naturalKeySort(it.light.config["number"]?.toString() ?: "") }
            ))
        }

        return lightMap
        */
        return emptyList()
    }

    companion object {
        /**
         * Sort by natural keys like humans do.
         *
         * Splits string by digits to allow natural sorting (e.g., "item2" comes before "item10").
         *
         * @param stringToSort String to convert to natural sort key
         * @return List of comparable parts for sorting
         *
         * See: http://www.codinghorror.com/blog/archives/001018.html
         */
        fun naturalKeySort(stringToSort: String): List<Comparable<*>> {
            val digitRegex = Regex("""(\d+)""")
            return digitRegex.split(stringToSort)
                .filter { it.isNotEmpty() }
                .map { part ->
                    part.toIntOrNull() ?: part
                }
        }
    }
}
