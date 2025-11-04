package org.missionpinball.mpf.core

import mu.KLogger
import mu.KotlinLogging
import org.missionpinball.mpf.Version
import org.missionpinball.mpf.exceptions.ConfigFileError

/**
 * Mixin class to add smart logging functionality to modules.
 *
 * In Kotlin, we use delegation pattern instead of Python's multiple inheritance.
 * This class provides logging capabilities that can be delegated to.
 */
interface LogMixin {
    val log: KLogger
    var infoToConsole: Boolean
    var debugToConsole: Boolean
    var debug: Boolean
    var infoToFile: Boolean
    var debugToFile: Boolean
    var info: Boolean
    var urlBase: String?

    fun configureLogging(
        loggerName: String,
        consoleLevel: String = "basic",
        fileLevel: String = "basic",
        urlBase: String? = null
    )

    fun debugLog(msg: String, vararg args: Any?, context: String? = null, errorNo: Int? = null)
    fun infoLog(msg: String, vararg args: Any?, context: String? = null, errorNo: Int? = null)
    fun warningLog(msg: String, vararg args: Any?, context: String? = null, errorNo: Int? = null)
    fun errorLog(msg: String, vararg args: Any?, context: String? = null, errorNo: Int? = null)

    fun raiseConfigError(msg: String, errorNo: Int, loggerOverride: String? = null): Nothing
}

/**
 * Default implementation of LogMixin.
 */
class LogMixinImpl : LogMixin {
    override val log: KLogger = KotlinLogging.logger {}
    override var infoToConsole: Boolean = false
    override var debugToConsole: Boolean = false
    override var debug: Boolean = false
    override var infoToFile: Boolean = false
    override var debugToFile: Boolean = false
    override var info: Boolean = false
    override var urlBase: String? = null

    private var configuredLogger: KLogger? = null

    override fun configureLogging(
        loggerName: String,
        consoleLevel: String,
        fileLevel: String,
        urlBase: String?
    ) {
        this.urlBase = urlBase ?: loggerName
        configuredLogger = KotlinLogging.logger(loggerName)

        when (consoleLevel.lowercase()) {
            "basic" -> {
                infoToConsole = true
                info = true
            }
            "full" -> {
                debugToConsole = true
                debug = true
            }
        }

        when (fileLevel.lowercase()) {
            "basic" -> {
                infoToFile = true
                info = true
            }
            "full" -> {
                debugToFile = true
                debug = true
            }
        }
    }

    override fun debugLog(msg: String, vararg args: Any?, context: String?, errorNo: Int?) {
        if (debug || debugToConsole) {
            val formattedMsg = if (args.isNotEmpty()) String.format(msg, *args) else msg
            configuredLogger?.debug { formattedMsg }
        }
    }

    override fun infoLog(msg: String, vararg args: Any?, context: String?, errorNo: Int?) {
        if (info || infoToConsole) {
            val formattedMsg = if (args.isNotEmpty()) String.format(msg, *args) else msg
            configuredLogger?.info { formattedMsg }
        }
    }

    override fun warningLog(msg: String, vararg args: Any?, context: String?, errorNo: Int?) {
        val formattedMsg = if (args.isNotEmpty()) String.format(msg, *args) else msg
        configuredLogger?.warn { formattedMsg }
    }

    override fun errorLog(msg: String, vararg args: Any?, context: String?, errorNo: Int?) {
        val formattedMsg = if (args.isNotEmpty()) String.format(msg, *args) else msg
        configuredLogger?.error { formattedMsg }
    }

    override fun raiseConfigError(msg: String, errorNo: Int, loggerOverride: String?): Nothing {
        val logger = loggerOverride ?: urlBase ?: "Unknown"
        throw ConfigFileError(msg, errorNo, logger)
    }
}
