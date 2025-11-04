package org.missionpinball.mpf.exceptions

/**
 * Runtime error in MPF or MPF-MC.
 */
class MpfRuntimeError(
    message: String,
    errorNo: Int,
    loggerName: String,
    context: String? = null,
    urlName: String? = null
) : BaseError(message, errorNo, loggerName, context, urlName) {

    override fun getShortName(): String = "RE"

    override fun getLongName(): String = "Runtime Error"
}
