package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.time.LocalDateTime
import java.time.Instant

/**
 * A periodic coroutine task.
 */
class PeriodicTask(
    private val intervalMs: Long,
    private val scope: CoroutineScope,
    private val callback: () -> Unit
) {
    private var job: Job? = null
    private var cancelled = false

    init {
        schedule()
    }

    private fun schedule() {
        if (cancelled) return

        job = scope.launch {
            delay(intervalMs)
            if (!cancelled) {
                callback()
                schedule()
            }
        }
    }

    /**
     * Cancel this periodic task.
     */
    fun cancel() {
        cancelled = true
        job?.cancel()
    }
}

/**
 * A clock object with event support using Kotlin Coroutines.
 * This replaces Python's asyncio event loop with Kotlin's coroutine system.
 */
class ClockBase(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
    private val logger = KotlinLogging.logger {}

    init {
        logger.debug { "Starting clock with coroutines" }
    }

    /**
     * Get the current time in milliseconds.
     */
    fun getTime(): Long = System.currentTimeMillis()

    /**
     * Get current datetime.
     */
    fun getDateTime(): LocalDateTime = LocalDateTime.now()

    /**
     * Schedule a callback to run once after a delay.
     */
    fun scheduleOnce(callback: () -> Unit, delaySeconds: Double): Job {
        val delayMs = (delaySeconds * 1000).toLong()
        return scope.launch {
            delay(delayMs)
            callback()
        }
    }

    /**
     * Schedule a callback to run periodically.
     */
    fun scheduleInterval(callback: () -> Unit, intervalSeconds: Double): PeriodicTask {
        val intervalMs = (intervalSeconds * 1000).toLong()
        return PeriodicTask(intervalMs, scope, callback)
    }

    /**
     * Unschedule a job.
     */
    fun unschedule(job: Job) {
        job.cancel()
    }
}
