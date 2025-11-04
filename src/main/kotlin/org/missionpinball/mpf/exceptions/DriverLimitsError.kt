package org.missionpinball.mpf.exceptions

/**
 * A request was made to drive the driver outside its configured limits.
 */
class DriverLimitsError(message: String) : AssertionError(message)
