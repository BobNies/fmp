package org.missionpinball.mpf.core

import kotlinx.coroutines.Job
import mu.KotlinLogging
import java.util.UUID

/**
 * Handles delays for one object.
 *
 * By default, a machine-wide instance is created and available.
 * Individual modes also have Delay Managers which are automatically
 * removed when the mode stops.
 */
class DelayManager(private val clock: ClockBase) {
    private val logger = KotlinLogging.logger {}
    private val delays = mutableMapOf<String, Pair<Job, () -> Unit>>()

    /**
     * Add a delay.
     *
     * @param ms The number of milliseconds you want this delay to be for.
     * @param callback The method that is called when this delay ends.
     * @param name String name of this delay. If not provided, a UUID will be created.
     * @return String name or UUID of the delay which you can use to remove it later.
     */
    fun add(ms: Long, callback: () -> Unit, name: String? = null): String {
        val delayName = name ?: UUID.randomUUID().toString()

        logger.debug { "Adding delay. Name: '$delayName' ms: $ms, callback: $callback" }

        // Remove existing delay with same name if it exists
        delays[delayName]?.let { (job, _) ->
            clock.unschedule(job)
        }

        val job = clock.scheduleOnce({
            processDelayCallback(delayName, callback)
        }, ms / 1000.0)

        delays[delayName] = Pair(job, callback)

        return delayName
    }

    /**
     * Remove a delay by name.
     *
     * Removing a delay prevents the callback from being called and cancels the delay.
     *
     * @param name String name of the delay you want to remove.
     */
    fun remove(name: String) {
        logger.debug { "Removing delay: '$name'" }
        delays.remove(name)?.let { (job, _) ->
            clock.unschedule(job)
        }
    }

    /**
     * Add a delay only if a delay with that name doesn't exist already.
     *
     * @param ms The number of milliseconds you want this delay to be for.
     * @param callback The method that is called when this delay ends.
     * @param name String name of this delay.
     * @return String name of the delay.
     */
    fun addIfDoesntExist(ms: Long, callback: () -> Unit, name: String): String {
        return if (name !in delays) {
            add(ms, callback, name)
        } else {
            logger.debug { "Delay '$name' already exists. Not adding." }
            name
        }
    }

    /**
     * Check if a delay exists.
     *
     * @param name The name of the delay to check.
     * @return True if the delay exists, False otherwise.
     */
    fun check(name: String): Boolean {
        return name in delays
    }

    /**
     * Reset all delays.
     * Cancels and removes all delays.
     */
    fun reset() {
        logger.debug { "Resetting DelayManager. Removing ${delays.size} delay(s)" }
        delays.values.forEach { (job, _) ->
            clock.unschedule(job)
        }
        delays.clear()
    }

    private fun processDelayCallback(name: String, callback: () -> Unit) {
        logger.debug { "Processing delay callback for '$name'" }
        delays.remove(name)
        callback()
    }
}
