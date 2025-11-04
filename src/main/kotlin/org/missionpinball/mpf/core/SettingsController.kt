package org.missionpinball.mpf.core

/**
 * Setting entry data class.
 *
 * @property name Setting name
 * @property label Display label for setting
 * @property sort Sort order
 * @property machineVar Machine variable name
 * @property default Default value
 * @property values Map of possible values to their labels
 * @property settingType Type of setting (standard, advanced, etc.)
 */
data class SettingEntry(
    val name: String,
    val label: String,
    val sort: Int,
    val machineVar: String,
    val default: Any,
    val values: Map<Any, String>,
    val settingType: String
)

/**
 * Manages operator controllable settings.
 *
 * Settings are stored as machine variables and can be configured
 * in the machine config or added programmatically. Each setting has
 * a set of allowed values and a default.
 */
class SettingsController(machine: MachineController) : MpfController(machine) {

    override val configName = "settings_controller"

    /**
     * Dictionary of available settings.
     */
    private val settings = mutableMapOf<String, SettingEntry>()

    init {
        addEntriesFromConfig()
    }

    /**
     * Add settings from machine config.
     */
    private fun addEntriesFromConfig() {
        // TODO: Implement when config system is complete
        /*
        val config = machine.config["settings"] as? Map<String, Any?> ?: emptyMap()

        for ((name, settingConfig) in config) {
            val validatedSettings = machine.configValidator.validateConfig("settings", settingConfig)

            val machineVar = (validatedSettings["machine_var"] as? String) ?: name

            // Convert types for values map
            val values = mutableMapOf<Any, String>()
            val keyType = validatedSettings["key_type"] as String
            for ((key, value) in validatedSettings["values"] as Map<*, *>) {
                val convertedKey = UtilityFunctions.convertToType(key.toString(), keyType)
                values[convertedKey] = value.toString()
            }

            // Convert default key
            val default = UtilityFunctions.convertToType(
                validatedSettings["default"].toString(),
                keyType
            )

            addSetting(SettingEntry(
                name = name,
                label = validatedSettings["label"] as String,
                sort = validatedSettings["sort"] as Int,
                machineVar = machineVar,
                default = default,
                values = values,
                settingType = validatedSettings["settingType"] as String
            ))
        }
        */
    }

    /**
     * Add a setting.
     *
     * @param setting Setting entry to add
     */
    fun addSetting(setting: SettingEntry) {
        settings[setting.name] = setting
    }

    /**
     * Get all available settings of a given type.
     *
     * @param settingType Type of settings to return (default: "standard")
     * @return List of settings sorted by sort order
     */
    fun getSettings(settingType: String = "standard"): List<SettingEntry> {
        return settings.values
            .filter { it.settingType == settingType }
            .sortedBy { it.sort }
    }

    /**
     * Get the label for the current value of a setting.
     *
     * @param settingName Name of setting
     * @return Label for current value
     */
    fun getSettingValueLabel(settingName: String): String {
        val value = getSettingValue(settingName)
        return settings[settingName]?.values?.get(value) ?: "invalid"
    }

    /**
     * Get the machine variable name for a setting.
     *
     * @param settingName Name of setting
     * @return Machine variable name
     */
    fun getSettingMachineVar(settingName: String): String {
        return settings[settingName]?.machineVar
            ?: throw IllegalArgumentException("Invalid setting $settingName")
    }

    /**
     * Get the current value of a setting.
     *
     * @param settingName Name of setting
     * @return Current value (or default if not set)
     */
    fun getSettingValue(settingName: String): Any {
        val setting = settings[settingName]
            ?: throw IllegalArgumentException("Invalid setting $settingName")

        // TODO: Get from machine variables when implemented
        /*
        val value = if (!machine.variables.isMachineVar(setting.machineVar)) {
            setting.default
        } else {
            machine.variables.getMachineVar(setting.machineVar)
        }

        if (value !in setting.values) {
            warningLog(
                "Setting $settingName contained an invalid value $value. " +
                "Falling back to default: ${setting.default}"
            )
            return setting.default
        }

        debugLog("Retrieving value: $settingName=$value")
        return value
        */

        // Stub implementation
        return setting.default
    }

    /**
     * Set the value of a setting.
     *
     * @param settingName Name of setting
     * @param value New value
     */
    fun setSettingValue(settingName: String, value: Any) {
        debugLog("New value: $settingName=$value")

        val setting = settings[settingName]
            ?: throw IllegalArgumentException("Invalid setting $settingName")

        if (value !in setting.values) {
            throw IllegalArgumentException("Invalid value $value for setting $settingName")
        }

        // TODO: Set machine variable when implemented
        /*
        machine.variables.configureMachineVar(name = setting.machineVar, persist = true)
        machine.variables.setMachineVar(name = setting.machineVar, value = value)
        */
    }
}
