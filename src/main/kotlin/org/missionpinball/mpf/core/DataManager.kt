package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.createDirectories

/**
 * Handles key-value data loading and saving for the machine.
 *
 * The DataManager is responsible for reading and writing data to/from a
 * file on disk. It uses a background coroutine to batch writes and prevent
 * excessive disk I/O.
 *
 * @param machine The main MachineController instance
 * @param name A string name that represents what this DataManager instance is for
 * @param minWaitSecs Minimal seconds to wait between two writes
 */
class DataManager(
    machine: MachineController,
    val name: String,
    private val minWaitSecs: Int = 1
) : MpfController(machine) {

    override val configName = "data_manager"

    /**
     * Filename for this data manager.
     */
    val filename: String?

    /**
     * Data dictionary.
     */
    val data = mutableMapOf<String, Any?>()

    /**
     * Whether data needs to be written to disk.
     */
    private val dirty = AtomicBoolean(false)

    /**
     * Background coroutine job for writing.
     */
    private var writingJob: Job? = null

    /**
     * Scope for background coroutines.
     */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // TODO: Get path from machine config when config system is complete
        /*
        val configPath = machine.config["mpf"]?.get("paths")?.get(name)
        filename = when {
            configPath == false -> null
            configPath is String && configPath.startsWith("/") -> configPath
            configPath is String -> File(machine.machinePath, configPath).absolutePath
            else -> throw IllegalArgumentException("Invalid path $configPath for $name")
        }
        */
        // Temporary stub path
        filename = null

        if (filename != null) {
            setupFile()
            writingJob = scope.launch {
                writingThread()
            }
        }
    }

    /**
     * Setup the file (create directory if needed and load existing data).
     */
    private fun setupFile() {
        if (filename != null) {
            makeSurePathExists(File(filename).parent)
            load()
        }
    }

    /**
     * Create directory path if it doesn't exist.
     */
    private fun makeSurePathExists(path: String) {
        try {
            Path.of(path).createDirectories()
        } catch (e: Exception) {
            // Ignore if directory already exists
            if (!Files.isDirectory(Path.of(path))) {
                throw e
            }
        }
    }

    /**
     * Load data from disk.
     */
    private fun load() {
        if (filename == null) return

        debugLog("Loading $name from $filename")

        val file = File(filename)
        if (file.isFile) {
            try {
                val loadedData = FileManager.load(filename, haltOnError = false)
                data.clear()
                data.putAll(loadedData)
            } catch (e: Exception) {
                logger.warn(e) { "Error loading $name from $filename" }
            }
        } else {
            debugLog("Didn't find the $name file. No prob. We'll create it when we save.")
        }

        if (data.isEmpty()) {
            data.clear()
        }
    }

    /**
     * Get data from this DataManager.
     *
     * @param section Optional section name to get specific data
     * @return Data dictionary (or empty dict if section not found)
     */
    fun getData(section: String? = null): Map<String, Any?> {
        return if (section == null) {
            data.toMap()
        } else {
            val sectionData = data[section]
            when (sectionData) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    (sectionData as Map<String, Any?>).toMap()
                }
                else -> emptyMap()
            }
        }
    }

    /**
     * Trigger a write of this DataManager's data to disk.
     */
    private fun triggerSave() {
        debugLog("Will write $name to disk")
        dirty.set(true)
    }

    /**
     * Update all data and trigger save.
     *
     * @param newData New data to save
     */
    fun saveAll(newData: Map<String, Any?>) {
        data.clear()
        data.putAll(newData)
        triggerSave()
    }

    /**
     * Background writing thread.
     *
     * Waits for dirty flag and writes data to disk, with rate limiting.
     */
    private suspend fun writingThread() {
        // Prevent early writes at start-up
        delay((minWaitSecs * 1000).toLong())

        var lastData: Map<String, Any?>? = null

        while (isActive) {
            // Wait up to 1 second or until dirty flag is set
            delay(1000)

            if (!dirty.get()) {
                continue
            }

            // Wait for FileManager to not be busy
            while (FileManager.isBusy) {
                delay(200)
            }

            dirty.set(false)

            // Make a deep copy of data
            lastData = data.toMap()

            // Save data
            try {
                if (filename != null && lastData.isNotEmpty()) {
                    FileManager.save(filename, lastData)
                }
            } catch (e: Exception) {
                // If the file writer has an exception, handle it here. Otherwise
                // this coroutine will die and all subsequent write attempts will no-op.
                logger.warn(e) { "ERROR writing file $filename" }
            }

            lastData = null

            // Prevent too many writes
            delay((minWaitSecs * 1000).toLong())
        }

        // If dirty, write data one last time during shutdown
        if (lastData != null && dirty.get()) {
            while (FileManager.isBusy) {
                delay(200)
            }
            try {
                if (filename != null && lastData.isNotEmpty()) {
                    FileManager.save(filename, lastData)
                }
            } catch (e: Exception) {
                logger.error(e) { "ERROR writing file during shutdown $filename" }
            }
        }
    }

    /**
     * Stop the DataManager and cancel the writing coroutine.
     */
    fun stop() {
        writingJob?.cancel()
        scope.cancel()
    }
}
