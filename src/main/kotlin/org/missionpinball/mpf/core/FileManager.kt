package org.missionpinball.mpf.core

import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val logger = KotlinLogging.logger("FileManager")

/**
 * Interface for file loading and saving.
 */
interface FileInterface {
    /**
     * File extensions this interface handles.
     */
    val fileTypes: List<String>

    /**
     * Find a file, optionally testing with extensions.
     *
     * @param filename Filename to find
     * @return Pair of (full path, extension) or null if not found
     */
    fun findFile(filename: String): Pair<String, String>? {
        val file = File(filename)
        val extension = file.extension

        if (extension.isEmpty()) {
            // File has no extension, try all supported extensions
            for (ext in fileTypes) {
                val testFile = File(filename + ext)
                if (testFile.isFile) {
                    return Pair(testFile.absolutePath, ext)
                }
            }
            return null
        }

        return if (file.isFile) {
            Pair(file.absolutePath, ".$extension")
        } else {
            null
        }
    }

    /**
     * Load data from a file.
     *
     * @param filename File to load
     * @param verifyVersion Whether to verify version string
     * @param haltOnError Whether to throw exception on error or return empty dict
     * @return Loaded data
     */
    fun load(filename: String, verifyVersion: Boolean = false, haltOnError: Boolean = true): Map<String, Any?>

    /**
     * Save data to a file.
     *
     * @param filename File to save to
     * @param data Data to save
     */
    fun save(filename: String, data: Map<String, Any?>)
}

/**
 * YAML file interface stub.
 *
 * TODO: Implement full YAML support using Kaml library.
 */
class YamlInterface : FileInterface {
    override val fileTypes = listOf(".yaml", ".yml")

    override fun load(filename: String, verifyVersion: Boolean, haltOnError: Boolean): Map<String, Any?> {
        // TODO: Implement YAML loading with Kaml
        logger.warn { "YAML loading not yet implemented, returning empty map for $filename" }
        return emptyMap()
    }

    override fun save(filename: String, data: Map<String, Any?>) {
        // TODO: Implement YAML saving with Kaml
        logger.warn { "YAML saving not yet implemented for $filename" }
    }
}

/**
 * Pickle (binary) file interface stub.
 *
 * TODO: Implement binary serialization using Kotlin serialization.
 */
class PickleInterface : FileInterface {
    override val fileTypes = listOf(".bin")

    override fun load(filename: String, verifyVersion: Boolean, haltOnError: Boolean): Map<String, Any?> {
        // TODO: Implement binary loading
        logger.warn { "Binary loading not yet implemented, returning empty map for $filename" }
        return emptyMap()
    }

    override fun save(filename: String, data: Map<String, Any?>) {
        // TODO: Implement binary saving
        logger.warn { "Binary saving not yet implemented for $filename" }
    }
}

/**
 * Manages file interfaces for loading and saving configuration and data files.
 *
 * Singleton that handles file operations with support for multiple file formats.
 */
object FileManager {

    /**
     * Map of file extensions to their interfaces.
     */
    private val fileInterfaces = mutableMapOf<String, FileInterface>()

    /**
     * Whether the FileManager has been initialized.
     */
    var initialized = false
        private set

    /**
     * Whether a file operation is currently in progress.
     * Used to prevent concurrent writes.
     */
    @Volatile
    var isBusy = false

    /**
     * Initialize file interfaces.
     */
    fun init() {
        fileInterfaces[".yaml"] = YamlInterface()
        fileInterfaces[".yml"] = YamlInterface()
        fileInterfaces[".bin"] = PickleInterface()
        initialized = true
    }

    /**
     * Locate a file, checking with various extensions if needed.
     *
     * @param filename Filename to locate
     * @return Absolute path to the file
     * @throws FileSystemException if file not found
     */
    fun locateFile(filename: String): String {
        require(filename.isNotEmpty()) { "No filename provided" }

        if (!initialized) {
            init()
        }

        val file = File(filename)
        val extension = if (file.extension.isEmpty()) "" else ".${file.extension}"

        if (!file.isFile) {
            // If the file doesn't have an extension, try to find one
            if (extension.isEmpty()) {
                for (configProcessor in fileInterfaces.values.toSet()) {
                    val result = configProcessor.findFile(filename)
                    if (result != null) {
                        return result.first
                    }
                }
            }

            throw java.nio.file.FileSystemException("File not found: $filename")
        }

        return file.absolutePath
    }

    /**
     * Get the file interface for a given filename.
     *
     * @param filename Filename
     * @return File interface or null if not found
     */
    fun getFileInterface(filename: String): FileInterface? {
        return try {
            val locatedFile = locateFile(filename)
            val ext = File(locatedFile).let {
                if (it.extension.isEmpty()) "" else ".${it.extension}"
            }
            fileInterfaces[ext]
        } catch (e: java.nio.file.FileSystemException) {
            null
        }
    }

    /**
     * Load a file by name.
     *
     * @param filename File to load
     * @param verifyVersion Whether to verify version string
     * @param haltOnError Whether to throw exception on error
     * @return Loaded data map
     */
    fun load(filename: String, verifyVersion: Boolean = false, haltOnError: Boolean = true): Map<String, Any?> {
        if (!initialized) {
            init()
        }

        val file = try {
            locateFile(filename)
        } catch (e: java.nio.file.FileSystemException) {
            return if (haltOnError) {
                throw IllegalStateException("Could not find file $filename")
            } else {
                emptyMap()
            }
        }

        val ext = File(file).let {
            if (it.extension.isEmpty()) "" else ".${it.extension}"
        }

        val interface_ = fileInterfaces[ext]
            ?: throw IllegalStateException("No config file processor available for file type $ext")

        return interface_.load(file, verifyVersion, haltOnError)
    }

    /**
     * Save data to a file.
     *
     * Uses atomic write (save to temp file, then move) to prevent corruption.
     *
     * @param filename File to save to
     * @param data Data to save
     */
    fun save(filename: String, data: Map<String, Any?>) {
        if (!initialized) {
            init()
        }

        // Set busy flag to prevent concurrent writes
        isBusy = true

        try {
            val file = File(filename)
            val ext = if (file.extension.isEmpty()) "" else ".${file.extension}"

            val interface_ = fileInterfaces[ext]
                ?: throw IllegalStateException("No config file processor available for file type $ext")

            // Save to temp file and move afterwards to prevent broken files
            val tempFile = File(file.parent, "_${file.name}")

            interface_.save(tempFile.absolutePath, data)

            // Move temp file to final location (atomic on most systems)
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } finally {
            isBusy = false
        }
    }
}
