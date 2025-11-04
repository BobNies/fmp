package org.missionpinball.mpf.exceptions

/**
 * Error in a config file found.
 */
class ConfigFileError(
    message: String,
    errorNo: Int,
    loggerName: String,
    context: String? = null,
    urlName: String? = null
) : BaseError(message, errorNo, loggerName, context, urlName) {

    override fun getShortName(): String = "CFE"

    override fun getLongName(): String = "Config File Error"
}
