package org.missionpinball.mpf.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import mu.KotlinLogging
import org.missionpinball.mpf.Version
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Base command for MPF CLI.
 */
class MpfCommand : CliktCommand(
    name = "mpf",
    help = "Mission Pinball Framework - Control your pinball machine"
) {
    private val version by option("--version", help = "Show version information").flag()
    private val machinePath by argument("machine_path", help = "Path to machine folder").optional()
    private val production by option("-P", "--production", help = "Production mode").flag()

    override fun run() {
        if (version) {
            echo(Version.extendedVersion)
            return
        }

        logger.info { "MPF Starting..." }
        logger.info { "Machine path: ${machinePath ?: "current directory"}" }
        if (production) {
            logger.info { "Running in production mode" }
        }

        // Machine path resolution logic
        val resolvedPath = resolveMachinePath(machinePath)
        logger.info { "Resolved machine path: $resolvedPath" }

        // TODO: Initialize machine controller and start game
        echo("MPF initialized successfully (Kotlin version)")
        echo("Machine path: $resolvedPath")
    }

    private fun resolveMachinePath(pathHint: String?): String {
        val currentDir = File(System.getProperty("user.dir"))

        // If path hint provided, use it
        if (pathHint != null) {
            val hintFile = File(pathHint)
            if (hintFile.isDirectory) {
                return hintFile.absolutePath
            }
        }

        // Check if current directory has a config folder
        val configDir = File(currentDir, "config")
        if (configDir.exists() && configDir.isDirectory) {
            return currentDir.absolutePath
        }

        throw IllegalArgumentException(
            "Error: Could not find machine in folder: '${currentDir.absolutePath}'. " +
            "Either start MPF from within your machine root folder or provide the path after the command."
        )
    }
}

/**
 * Game command - runs the pinball game.
 */
class GameCommand : CliktCommand(name = "game", help = "Run the pinball game") {
    override fun run() {
        logger.info { "Starting game mode..." }
        echo("Game command not yet fully implemented in Kotlin version")
        // TODO: Implement game logic
    }
}

/**
 * Hardware command - test hardware connections.
 */
class HardwareCommand : CliktCommand(name = "hardware", help = "Test hardware connections") {
    override fun run() {
        logger.info { "Starting hardware test mode..." }
        echo("Hardware command not yet fully implemented in Kotlin version")
        // TODO: Implement hardware testing logic
    }
}

/**
 * Service command - service menu for pinball machine.
 */
class ServiceCommand : CliktCommand(name = "service", help = "Service menu") {
    override fun run() {
        logger.info { "Starting service mode..." }
        echo("Service command not yet fully implemented in Kotlin version")
        // TODO: Implement service menu logic
    }
}

/**
 * Command line utility for MPF.
 * Handles parsing and dispatching of CLI commands.
 */
class CommandLineUtility(private val args: Array<String>) {

    fun execute() {
        val mpfCommand = MpfCommand()
            .subcommands(
                GameCommand(),
                HardwareCommand(),
                ServiceCommand()
            )

        mpfCommand.main(args.toList())
    }
}
