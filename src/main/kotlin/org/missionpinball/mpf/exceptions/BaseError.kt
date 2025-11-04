package org.missionpinball.mpf.exceptions

import org.missionpinball.mpf.Version

/**
 * Base error class for MPF and MPF-MC errors.
 */
abstract class BaseError(
    private var message: String,
    private val errorNo: Int,
    private val loggerName: String,
    private val context: String? = null,
    private val urlName: String? = null
) : AssertionError(message) {

    private val effectiveUrlName: String = urlName ?: loggerName

    /**
     * Get the error number.
     */
    fun getErrorNo(): Int = errorNo

    /**
     * Get the error context.
     */
    fun getContext(): String? = context

    /**
     * Get the logger name.
     */
    fun getLoggerName(): String = loggerName

    /**
     * Get the short name for this error type.
     */
    abstract fun getShortName(): String

    /**
     * Get the long name for this error type.
     */
    abstract fun getLongName(): String

    /**
     * Chain a new message onto an existing error, keeping the original error's logger, context, and error_no.
     */
    fun extend(newMessage: String) {
        message = "$newMessage >> $message"
    }

    override fun toString(): String {
        val errorSlug = "${getShortName()}-${effectiveUrlName.replace(" ", "_")}-$errorNo"
        val errorUrl = "${Version.LOG_URL}/$errorSlug"

        return if (context != null) {
            "${getLongName()} in $loggerName: $message Context: $context Error Code: $errorSlug ($errorUrl)"
        } else {
            "${getLongName()} in $loggerName: $message Error Code: $errorSlug ($errorUrl)"
        }
    }
}
