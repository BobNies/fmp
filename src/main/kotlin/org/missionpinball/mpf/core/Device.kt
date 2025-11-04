package org.missionpinball.mpf.core

import kotlinx.coroutines.CompletableDeferred

/**
 * Generic parent class for every hardware device in a pinball machine.
 *
 * One instance of a Device subclass is created for each device in the machine.
 */
abstract class Device(
    protected val machine: MachineController,
    val name: String
) {
    // Logging functionality via delegation
    private val logMixin: LogMixin = LogMixinImpl()

    protected val log get() = logMixin.log
    protected val debug get() = logMixin.debug

    /**
     * String of the config section name.
     * Must be overridden in subclasses.
     */
    abstract val configSection: String

    /**
     * String name of the collection.
     * Must be overridden in subclasses.
     */
    abstract val collection: String

    /**
     * String of the friendly name of the device class.
     * Must be overridden in subclasses.
     */
    abstract val classLabel: String

    /**
     * Can a config for this device be empty?
     */
    open val allowEmptyConfigs: Boolean = false

    /**
     * List of tags applied to this device.
     */
    val tags: MutableList<String> = mutableListOf()

    /**
     * Platform for this device (hardware interface).
     */
    var platform: Any? = null

    /**
     * User-friendly label for this device.
     */
    var label: String? = null

    /**
     * Validated dictionary of this device's settings.
     * Maps to the YAML-based config specified in the Config Spec.
     */
    var config: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Compare two devices by name.
     */
    operator fun compareTo(other: Device): Int {
        return name.compareTo(other.name)
    }

    /**
     * Add a device to a running mode.
     *
     * @param mode Mode which loaded the device.
     * @throws IllegalStateException if device cannot be used in mode.
     */
    open suspend fun deviceAddedToMode(mode: Any) {
        throw IllegalStateException("Cannot use device $name in mode.")
    }

    /**
     * Called when device is added system-wide.
     */
    open suspend fun deviceAddedSystemWide() {
        // Initialize the device
        initialize()
    }

    /**
     * Get the config spec for this device.
     * Override in subclasses that have config specs.
     */
    open fun getConfigSpec(): Any? = null

    /**
     * Can be called in initialize() to load the platform section.
     *
     * @param platformSection Name of the platform section.
     */
    protected fun loadPlatformSection(platformSection: String) {
        val platformName = config["platform"] as? String
        // TODO: Implement when platform controller is available
        // platform = machine.getPlatformSections(platformSection, platformName)
    }

    /**
     * Return the config prepared for validation.
     *
     * @param deviceConfig Config of device.
     * @param isModeConfig Whether this device is loaded in a mode or system-wide.
     * @return Prepared config.
     */
    open fun prepareConfig(deviceConfig: Map<String, Any?>, isModeConfig: Boolean): Map<String, Any?> {
        return deviceConfig
    }

    /**
     * Return the parsed and validated config.
     *
     * @param deviceConfig Config of device.
     * @param isModeConfig Whether this device is loaded in a mode or system-wide.
     * @param debugPrefix Prefix to use when logging.
     * @return Validated config.
     */
    open fun validateAndParseConfig(
        deviceConfig: MutableMap<String, Any?>,
        isModeConfig: Boolean,
        debugPrefix: String? = null
    ): Map<String, Any?> {
        // TODO: Implement config validation when config_validator is available
        // machine.configValidator.validateConfig(configSection, deviceConfig, name, "device", prefix = debugPrefix)

        configureDeviceLogging(deviceConfig)

        return deviceConfig
    }

    /**
     * Configure logging for this device.
     */
    private fun configureDeviceLogging(deviceConfig: Map<String, Any?>) {
        var consoleLog = deviceConfig["console_log"] as? String ?: "basic"
        var fileLog = deviceConfig["file_log"] as? String ?: "basic"

        // If debug is enabled, set logging to full
        if (deviceConfig["debug"] as? Boolean == true) {
            consoleLog = "full"
            fileLog = "full"
        }

        logMixin.configureLogging(
            "$classLabel.$name",
            consoleLog,
            fileLog,
            classLabel
        )

        debugLog("Configuring device with settings: '$deviceConfig'")
    }

    /**
     * Load config for this device.
     *
     * @param deviceConfig Config for device.
     */
    open fun loadConfig(deviceConfig: Map<String, Any?>) {
        config = deviceConfig.toMutableMap()

        // Load tags
        @Suppress("UNCHECKED_CAST")
        (deviceConfig["tags"] as? List<String>)?.let {
            tags.clear()
            tags.addAll(it)
        }

        // Load label
        label = deviceConfig["label"] as? String
    }

    /**
     * Initialize device.
     * Override in subclasses to perform device-specific initialization.
     */
    protected open suspend fun initialize() {
        // Subclasses should override this
    }

    /**
     * Stop device.
     * Override in subclasses to perform cleanup when device is stopped.
     */
    open fun stopDevice() {
        // Subclasses can override this
    }

    override fun toString(): String {
        return "<$classLabel.$name>"
    }

    /**
     * Get config collection and config section.
     *
     * @return Pair with (collection, configSection).
     */
    fun getConfigInfo(): Pair<String, String> {
        require(collection.isNotEmpty() && configSection.isNotEmpty()) {
            "Implement collection and configSection in ${this::class.simpleName}"
        }
        return Pair(collection, configSection)
    }

    // Delegate logging methods to logMixin
    protected fun debugLog(msg: String, vararg args: Any?) {
        logMixin.debugLog(msg, *args)
    }

    protected fun infoLog(msg: String, vararg args: Any?) {
        logMixin.infoLog(msg, *args)
    }

    protected fun warningLog(msg: String, vararg args: Any?) {
        logMixin.warningLog(msg, *args)
    }

    protected fun errorLog(msg: String, vararg args: Any?) {
        logMixin.errorLog(msg, *args)
    }

    protected fun raiseConfigError(msg: String, errorNo: Int, loggerOverride: String? = null): Nothing {
        logMixin.raiseConfigError(msg, errorNo, loggerOverride)
    }

    companion object {
        /**
         * Device class initialization.
         * Called once per device class when devices are being created.
         * Override in subclasses that need class-level initialization.
         */
        @JvmStatic
        open fun deviceClassInit(machine: MachineController) {
            // Subclasses can override this
        }
    }
}
