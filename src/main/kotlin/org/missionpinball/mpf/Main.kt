package org.missionpinball.mpf

import org.missionpinball.mpf.commands.CommandLineUtility
import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for the Mission Pinball Framework (Kotlin version).
 *
 * This is the Kotlin conversion of the Python MPF framework.
 * Command line arguments are processed and dispatched to appropriate handlers.
 */
fun main(args: Array<String>) {
    logger.info { "Mission Pinball Framework ${Version.version}" }
    logger.info { Version.extendedVersion }

    try {
        // Initialize command line utility and execute
        val utility = CommandLineUtility(args)
        utility.execute()
    } catch (e: Exception) {
        logger.error(e) { "Error running MPF: ${e.message}" }
        exitProcess(1)
    }
}
