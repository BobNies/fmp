package org.missionpinball.mpf

/**
 * Holds various Version strings of MPF.
 *
 * This object holds the MPF version strings, including the version of BCP it
 * needs and the config file version it needs.
 *
 * It's used internally for all sorts of things, from printing the output of the
 * `mpf --version` command, to making sure any processes connected via BCP are
 * the proper versions, to automatically triggering new builds and deployments.
 */
object Version {
    /** The full version of MPF. */
    const val VERSION = "0.57.4.dev2"

    /** The major.minor version of MPF. */
    const val SHORT_VERSION = "0.57"

    /** The version of BCP this build of MPF uses. */
    const val BCP_VERSION = "1.1"

    /** The config file version this build of MPF uses. */
    const val CONFIG_VERSION = "6"

    /** The show format version this build of MPF uses. */
    const val SHOW_VERSION = "6"

    /** A friendly version string for this build of MPF. */
    val version: String
        get() = "Mission Pinball Framework v$VERSION"

    /** An extended version string that includes the MPF version, show version, and BCP versions. */
    val extendedVersion: String
        get() = "Mission Pinball Framework v$VERSION, Config version:$CONFIG_VERSION, " +
                "Show version: $SHOW_VERSION, BCP version:$BCP_VERSION"

    /** URL for log documentation */
    const val LOG_URL = "https://missionpinball.org/logs"
}
