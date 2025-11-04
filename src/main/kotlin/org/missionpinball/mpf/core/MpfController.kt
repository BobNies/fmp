package org.missionpinball.mpf.core

/**
 * Base class for MPF controllers.
 *
 * This is an abstract base class that all MPF controllers inherit from.
 * It provides logging functionality and access to the machine controller.
 */
abstract class MpfController(
    protected val machine: MachineController
) {
    // Logging functionality via delegation
    private val logMixin: LogMixin = LogMixinImpl()

    protected val log get() = logMixin.log
    protected val debug get() = logMixin.debug

    /**
     * Module name for logging purposes.
     * Override in subclasses to provide a specific name.
     */
    open val moduleName: String
        get() = this::class.simpleName ?: "Unknown"

    /**
     * Config name for this controller.
     * Must be overridden in subclasses.
     */
    abstract val configName: String

    init {
        require(configName.isNotEmpty()) {
            "Please specify a config name for $this"
        }

        logMixin.configureLogging(
            moduleName,
            machine.config["logging"]?.get("console")?.get(configName) as? String ?: "basic",
            machine.config["logging"]?.get("file")?.get(configName) as? String ?: "basic"
        )

        debugLog("Loading the $moduleName")
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
}

/**
 * Placeholder for MachineController.
 * This will be properly implemented when we convert machine.py
 */
class MachineController {
    val config: Map<String, Any> = mapOf(
        "logging" to mapOf(
            "console" to emptyMap<String, String>(),
            "file" to emptyMap<String, String>()
        )
    )

    val clock: ClockBase = ClockBase()
}
