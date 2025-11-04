package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Key for identifying registered event handlers.
 */
data class EventHandlerKey(
    val key: UUID,
    val event: String
)

/**
 * Registered handler information.
 */
data class RegisteredHandler(
    val callback: (Map<String, Any?>) -> Any?,
    val priority: Int,
    val kwargs: Map<String, Any?>,
    val key: UUID,
    val condition: Any? = null,  // Placeholder for template conditions
    val blockingFacility: Any? = null
)

/**
 * Posted event information.
 */
data class PostedEvent(
    val event: String,
    val type: String?,
    val callback: ((Map<String, Any?>) -> Unit)?,
    val kwargs: Map<String, Any?>
)

/**
 * Exception from within an event handler.
 */
class EventHandlerException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Base class for a queued event.
 */
class QueuedEvent(private val debugLog: (String) -> Unit) {
    var waiter: Boolean = false
        private set
    private var event: CompletableDeferred<Unit>? = null

    /**
     * Register a wait for this QueuedEvent.
     */
    fun wait() {
        require(!waiter) { "Double lock" }
        waiter = true
        debugLog("QueuedEvent: Registering a wait.")
    }

    /**
     * Clear a wait.
     */
    fun clear() {
        require(waiter) { "Not locked" }
        waiter = false
        event?.complete(Unit)
    }

    /**
     * Check if the queue is empty (unlocked).
     */
    fun isEmpty(): Boolean = !waiter

    /**
     * Suspend until the wait is cleared.
     */
    suspend fun awaitClear() {
        event = CompletableDeferred()
        event?.await()
    }

    override fun toString(): String = "<QueuedEvent>"
}

/**
 * Handles all the events and manages the handlers in MPF.
 *
 * This is the Kotlin implementation of Python's EventManager using coroutines.
 */
class EventManager(machine: MachineController) : MpfController(machine) {

    override val configName = "event_manager"

    private val registeredHandlers = ConcurrentHashMap<String, MutableList<RegisteredHandler>>()
    private val eventQueue = ArrayDeque<PostedEvent>()
    private val callbackQueue = ArrayDeque<Pair<(Map<String, Any?>) -> Unit, Map<String, Any?>>>()
    var monitorEvents: Boolean = false
    private val queueTasks = mutableListOf<Job>()
    private var stopped = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        addHandler("debug_dump_stats", { debugDumpEvents() })
    }

    private fun debugDumpEvents() {
        log.info { "--- DEBUG DUMP EVENTS ---" }
        log.info {
            "Total registered_handlers: ${registeredHandlers.size}. " +
            "Total event_queue: ${eventQueue.size}. Total callback_queue: ${callbackQueue.size}. " +
            "Total _queue_tasks: ${queueTasks.size}"
        }
        log.info { "Registered Handlers:" }
        val handlers = registeredHandlers.entries.sortedByDescending { it.value.size }
        for ((eventName, eventList) in handlers) {
            log.info { "  Total handlers: ${eventList.size} (for $eventName)" }
        }
        log.info { "Queue events:" }
        for (eventTask in queueTasks) {
            log.info { " $eventTask:" }
        }
        log.info { "--- DEBUG DUMP EVENTS END ---" }
    }

    /**
     * Parse an event string to divide the event name from a possible placeholder/conditional in braces.
     */
    fun getEventAndConditionFromString(eventString: String): Triple<String, Any?, Int> {
        var event = eventString
        var placeholder: Any? = null
        var additionalPriority = 0

        // Handle conditional in braces
        if (event.endsWith("}")) {
            val firstBracketPos = event.indexOf("{")
            require(firstBracketPos >= 0) {
                "Failed to parse condition in event name, please remedy \"$eventString\""
            }
            require(!event.substring(0, firstBracketPos).contains(" ")) {
                "Cannot handle events with spaces in the event name, please remedy \"$eventString\""
            }
            // TODO: Build placeholder template when placeholder_manager is converted
            event = event.substring(0, firstBracketPos)
        } else {
            require(!event.contains(" ")) {
                "Cannot handle events with spaces in the event name, please remedy \"$eventString\""
            }
            require(!event.contains("{")) {
                "Failed to parse condition in event name, please remedy \"$eventString\""
            }
        }

        // Handle priority suffix
        val priorityStart = event.indexOf(".")
        if (priorityStart > 0) {
            try {
                additionalPriority = event.substring(priorityStart + 1).toInt()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException(
                    "Failed to parse priority in event name, please remedy \"$eventString\". " +
                    "Does your event name contain a dot?", e
                )
            }
            event = event.substring(0, priorityStart)
        }

        return Triple(event, placeholder, additionalPriority)
    }

    /**
     * Register a coroutine as an async event handler.
     */
    fun addAsyncHandler(
        event: String,
        handler: suspend (Map<String, Any?>) -> Any?,
        priority: Int = 1,
        blockingFacility: Any? = null,
        vararg kwargs: Pair<String, Any?>
    ): EventHandlerKey {
        val asyncWrapper: (Map<String, Any?>) -> Any? = { params ->
            val queue = params["queue"] as? QueuedEvent
            queue?.wait()
            scope.launch {
                try {
                    handler(params)
                } finally {
                    queue?.clear()
                }
            }
            null
        }
        return addHandler(event, asyncWrapper, priority, blockingFacility, *kwargs)
    }

    /**
     * Register an event handler to respond to an event.
     *
     * @param event String name of the event you're adding a handler for.
     * @param handler The callable method that will be called when the event is fired.
     * @param priority An arbitrary integer value that defines what order the handlers will be called in.
     * @param blockingFacility Facility which can block this event.
     * @param kwargs Additional keyword/argument pairs attached to the handler.
     * @return EventHandlerKey to the handler which you can use to later remove the handler.
     */
    fun addHandler(
        event: String,
        handler: (Map<String, Any?>) -> Any?,
        priority: Int = 1,
        blockingFacility: Any? = null,
        vararg kwargs: Pair<String, Any?>
    ): EventHandlerKey {
        require(event.isNotEmpty()) { "Cannot pass event None or empty string." }

        val (parsedEvent, condition, additionalPriority) = getEventAndConditionFromString(event)
        val finalPriority = priority + additionalPriority

        val key = UUID.randomUUID()
        val kwargMap = kwargs.toMap()

        val registeredHandler = RegisteredHandler(
            callback = handler,
            priority = finalPriority,
            kwargs = kwargMap,
            key = key,
            condition = condition,
            blockingFacility = blockingFacility
        )

        // Get or create handler list for this event
        val handlerList = registeredHandlers.getOrPut(parsedEvent) { mutableListOf() }
        handlerList.add(registeredHandler)

        if (debug) {
            debugLog(
                "Registered %s as a handler for '%s', priority: %s, kwargs: %s",
                handler.toString(), parsedEvent, finalPriority, kwargMap
            )
        }

        // Sort handlers by priority (highest first)
        if (handlerList.size > 1) {
            handlerList.sortByDescending { it.priority }
        }

        return EventHandlerKey(key, parsedEvent)
    }

    /**
     * Replace an existing handler or add a new one.
     */
    fun replaceHandler(
        event: String,
        handler: (Map<String, Any?>) -> Any?,
        priority: Int = 1,
        vararg kwargs: Pair<String, Any?>
    ): EventHandlerKey {
        registeredHandlers[event]?.let { handlers ->
            val kwargMap = kwargs.toMap()
            handlers.removeAll { rh ->
                if (kwargMap.isEmpty()) {
                    rh.callback == handler
                } else {
                    rh.callback == handler && rh.kwargs == kwargMap
                }
            }
        }
        return addHandler(event, handler, priority, null, *kwargs)
    }

    /**
     * Remove all handlers for an event.
     */
    fun removeAllHandlersForEvent(event: String) {
        registeredHandlers.remove(event)
    }

    /**
     * Remove an event handler from all events.
     */
    fun removeHandler(method: (Map<String, Any?>) -> Any?) {
        val eventsToCheck = mutableListOf<String>()
        for ((event, handlerList) in registeredHandlers) {
            handlerList.removeAll { it.callback == method }
            if (debug) {
                debugLog("Removing method $method from event $event")
            }
            eventsToCheck.add(event)
        }
        eventsToCheck.forEach { removeEventIfEmpty(it) }
    }

    /**
     * Remove a handler from a specific event.
     */
    fun removeHandlerByEvent(event: String, handler: (Map<String, Any?>) -> Any?) {
        registeredHandlers[event]?.let { handlers ->
            handlers.removeAll { it.callback == handler }
            if (debug) {
                debugLog("Removing handler $handler from event $event")
            }
            removeEventIfEmpty(event)
        }
    }

    /**
     * Remove a registered event handler by key.
     */
    fun removeHandlerByKey(key: EventHandlerKey) {
        registeredHandlers[key.event]?.let { handlers ->
            handlers.removeAll { it.key == key.key }
            if (debug) {
                debugLog("Removing handler with key ${key.key} from event ${key.event}")
            }
            removeEventIfEmpty(key.event)
        }
    }

    /**
     * Remove multiple event handlers by keys.
     */
    fun removeHandlersByKeys(keyList: List<EventHandlerKey>) {
        keyList.forEach { removeHandlerByKey(it) }
    }

    private fun removeEventIfEmpty(event: String) {
        registeredHandlers[event]?.let { handlers ->
            if (handlers.isEmpty()) {
                registeredHandlers.remove(event)
                if (debug) {
                    debugLog("Removing event $event since there are no more handlers registered for it")
                }
            }
        }
    }

    /**
     * Wait for a specific event.
     */
    suspend fun waitForEvent(eventName: String): Map<String, Any?> {
        return waitForAnyEvent(listOf(eventName))
    }

    /**
     * Wait for any event from a list of event names.
     */
    suspend fun waitForAnyEvent(eventNames: List<String>): Map<String, Any?> {
        val deferred = CompletableDeferred<Map<String, Any?>>()
        val keys = mutableListOf<EventHandlerKey>()

        val waitHandler: (Map<String, Any?>) -> Any? = { kwargs ->
            keys.forEach { removeHandlerByKey(it) }
            if (!deferred.isCompleted) {
                deferred.complete(kwargs)
            }
            null
        }

        eventNames.forEach { eventName ->
            keys.add(addHandler(eventName, waitHandler))
        }

        return deferred.await()
    }

    /**
     * Check if any handlers are registered for an event.
     */
    fun doesEventExist(eventName: String): Boolean {
        return eventName in registeredHandlers
    }

    /**
     * Post an event and wait until all handlers are done.
     */
    suspend fun postAsync(event: String, vararg kwargs: Pair<String, Any?>): Map<String, Any?> {
        val deferred = CompletableDeferred<Map<String, Any?>>()
        val callback: (Map<String, Any?>) -> Unit = { result ->
            deferred.complete(result)
        }
        post(event, callback, *kwargs)
        return deferred.await()
    }

    /**
     * Post an event which causes all the registered handlers to be called.
     */
    fun post(event: String, callback: ((Map<String, Any?>) -> Unit)? = null, vararg kwargs: Pair<String, Any?>) {
        postInternal(event, null, callback, kwargs.toMap())
    }

    /**
     * Post a boolean event.
     * If any handler returns false, remaining handlers won't be called.
     */
    fun postBoolean(event: String, callback: ((Map<String, Any?>) -> Unit)? = null, vararg kwargs: Pair<String, Any?>) {
        postInternal(event, "boolean", callback, kwargs.toMap())
    }

    /**
     * Post a queue event.
     * Handlers can register waits, and callback is called when all waits are released.
     */
    fun postQueue(event: String, callback: (Map<String, Any?>) -> Unit, vararg kwargs: Pair<String, Any?>) {
        postInternal(event, "queue", callback, kwargs.toMap())
    }

    /**
     * Post a relay event.
     * Results from one handler are passed to the next handler.
     */
    fun postRelay(event: String, callback: ((Map<String, Any?>) -> Unit)? = null, vararg kwargs: Pair<String, Any?>) {
        postInternal(event, "relay", callback, kwargs.toMap())
    }

    private fun postInternal(
        event: String,
        evType: String?,
        callback: ((Map<String, Any?>) -> Unit)?,
        kwargs: Map<String, Any?>
    ) {
        if (stopped) {
            warningLog("Event after stop: ====='$event'===== Type: $evType, Callback: $callback, Args: $kwargs")
            return
        }

        if (debug) {
            debugLog("Event: ====='$event'===== Type: $evType, Callback: $callback, Args: $kwargs")
        }

        // Fast path for events without handlers
        if (callback == null && !monitorEvents && event !in registeredHandlers) {
            return
        }

        // Schedule event processing if queue was empty
        if (eventQueue.isEmpty()) {
            scope.launch {
                processEventQueue()
            }
        }

        val postedEvent = PostedEvent(event, evType, callback, kwargs)
        eventQueue.add(postedEvent)

        if (debug) {
            debugLog("+============= EVENTS QUEUE =============")
            eventQueue.forEach {
                debugLog("| ${it.event}, ${it.type}, ${it.callback}, ${it.kwargs}")
            }
            debugLog("+========================================")
        }
    }

    private suspend fun runHandlersSequential(
        event: String,
        callback: ((Map<String, Any?>) -> Unit)?,
        kwargs: Map<String, Any?>
    ) {
        if (debug) {
            debugLog("^^^^ Processing queue event '$event'. Callback: $callback, Args: $kwargs")
        }

        val handlers = registeredHandlers[event] ?: return

        for (handler in handlers.toList()) {
            val mergedKwargs = kwargs + handler.kwargs

            // Skip if condition exists and is not true
            if (handler.condition != null) {
                // TODO: Evaluate condition when placeholder manager is converted
                continue
            }

            if (debug) {
                debugLog(
                    "${handler.callback} (priority: ${handler.priority}) responding to event '$event' with args $mergedKwargs"
                )
            }

            val queue = mergedKwargs["queue"] as? QueuedEvent ?: QueuedEvent(::debugLog)
            val callKwargs = mergedKwargs.toMutableMap()
            callKwargs["queue"] = queue

            handler.callback(callKwargs)

            if (queue.waiter) {
                queue.awaitClear()
            }
        }

        if (debug) {
            debugLog("vvvv Finished queue event '$event'. Callback: $callback. Args: $kwargs")
        }

        callback?.invoke(kwargs)
    }

    private fun runHandlers(event: String, evType: String?, kwargs: Map<String, Any?>): Any? {
        var result: Any? = null
        val handlers = registeredHandlers[event] ?: return null

        for (handler in handlers.toList()) {
            val mergedKwargs = if (handler.kwargs.isNotEmpty() && kwargs.isNotEmpty()) {
                kwargs + handler.kwargs
            } else if (handler.kwargs.isNotEmpty()) {
                handler.kwargs
            } else {
                kwargs
            }

            // Skip if condition exists and is not true
            if (handler.condition != null) {
                // TODO: Evaluate condition when placeholder manager is converted
                continue
            }

            if (debug) {
                debugLog(
                    "${handler.callback} (priority: ${handler.priority}) responding to event '$event' with args $mergedKwargs"
                )
            }

            try {
                result = handler.callback(mergedKwargs)
            } catch (e: Exception) {
                throw EventHandlerException(
                    "Exception while processing $handler for event $event. ${e.message}",
                    e
                )
            }

            // Stop processing for boolean events if handler returns false
            if (evType == "boolean" && result == false) {
                if (debug) {
                    debugLog("Aborting future event processing")
                }
                break
            }

            // Update kwargs for relay events
            if (evType == "relay" && result is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                (kwargs as MutableMap<String, Any?>).putAll(result as Map<String, Any?>)
            }
        }

        return result
    }

    private fun processEvent(
        event: String,
        evType: String?,
        callback: ((Map<String, Any?>) -> Unit)?,
        kwargs: Map<String, Any?>
    ) {
        if (debug) {
            debugLog("^^^^ Processing event '$event'. Type: $evType, Callback: $callback, Args: $kwargs")
        }

        val result = if (event in registeredHandlers) {
            runHandlers(event, evType, kwargs)
        } else null

        if (debug) {
            debugLog("vvvv Finished event '$event'. Type: $evType. Callback: $callback. Args: $kwargs")
        }

        if (callback != null) {
            val callbackKwargs = kwargs.toMutableMap()
            if (result != null) {
                callbackKwargs["ev_result"] = result
            }
            callbackQueue.add(Pair(callback, callbackKwargs))
        }
    }

    private fun processQueueEvent(event: String, callback: ((Map<String, Any?>) -> Unit)?, kwargs: Map<String, Any?>) {
        if (event !in registeredHandlers) {
            // Fast path if there are no handlers
            callback?.let { callbackQueue.add(Pair(it, kwargs)) }
        } else {
            val task = scope.launch {
                runHandlersSequential(event, callback, kwargs)
            }
            task.invokeOnCompletion { exception ->
                exception?.let { logger.error(it) { "Queue task error" } }
                queueTasks.remove(task)
            }
            queueTasks.add(task)
        }
    }

    /**
     * Process all events in the queue.
     */
    private suspend fun processEventQueue() {
        val innerQueue = ArrayDeque<ArrayDeque<PostedEvent>>()

        while (eventQueue.isNotEmpty() || callbackQueue.isNotEmpty()) {
            // Process all events
            if (eventQueue.isNotEmpty()) {
                var nextQueue = eventQueue
                val newQueue = ArrayDeque<PostedEvent>()

                while (nextQueue.isNotEmpty()) {
                    val event = nextQueue.removeFirst()

                    if (nextQueue.isEmpty() && innerQueue.isNotEmpty()) {
                        nextQueue = innerQueue.removeFirst()
                    }

                    if (event.type == "queue") {
                        processQueueEvent(event.event, event.callback, event.kwargs)
                    } else {
                        processEvent(event.event, event.type, event.callback, event.kwargs)
                    }

                    // Handle events created during processing
                    if (eventQueue.isNotEmpty()) {
                        innerQueue.addFirst(nextQueue)
                        nextQueue = eventQueue
                        eventQueue.clear()
                        eventQueue.addAll(newQueue)
                        newQueue.clear()
                    }
                }
            }

            // Process callbacks
            if (callbackQueue.isNotEmpty()) {
                val (callback, kwargs) = callbackQueue.removeLast()
                callback(kwargs)
            }
        }
    }

    /**
     * Stop all ongoing event handlers.
     */
    fun stop() {
        stopped = true
        queueTasks.forEach { it.cancel() }
        queueTasks.clear()
    }

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        stop()
        scope.cancel()
    }
}
