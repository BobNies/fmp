using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace MPF.Core
{
    /// <summary>
    /// Event handler key for tracking and removing handlers
    /// </summary>
    public class EventHandlerKey
    {
        public Guid Key { get; private set; }
        public string Event { get; private set; }

        public EventHandlerKey(Guid key, string eventName)
        {
            Key = key;
            Event = eventName;
        }
    }

    /// <summary>
    /// Registered handler data structure
    /// </summary>
    public class RegisteredHandler
    {
        public Action<Dictionary<string, object>> Callback { get; set; }
        public int Priority { get; set; }
        public Dictionary<string, object> Kwargs { get; set; }
        public Guid Key { get; set; }
        public Func<bool> Condition { get; set; }
        public object BlockingFacility { get; set; }

        public RegisteredHandler(
            Action<Dictionary<string, object>> callback,
            int priority,
            Dictionary<string, object> kwargs,
            Guid key,
            Func<bool> condition = null,
            object blockingFacility = null)
        {
            Callback = callback;
            Priority = priority;
            Kwargs = kwargs ?? new Dictionary<string, object>();
            Key = key;
            Condition = condition;
            BlockingFacility = blockingFacility;
        }
    }

    /// <summary>
    /// Event manager for MPF - handles all events and registered handlers
    /// Converted from Python mpf/core/events.py
    /// </summary>
    public class EventManager : MonoBehaviour
    {
        private static EventManager _instance;
        public static EventManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    var go = new GameObject("EventManager");
                    _instance = go.AddComponent<EventManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        // Registered handlers dictionary: event name -> list of handlers
        private Dictionary<string, List<RegisteredHandler>> registeredHandlers =
            new Dictionary<string, List<RegisteredHandler>>();

        // Event queue for serial processing
        private Queue<PostedEvent> eventQueue = new Queue<PostedEvent>();

        // Flag to track if we're currently processing events
        private bool processingEvents = false;

        // Debug monitoring
        public bool MonitorEvents { get; set; } = false;
        public bool DebugMode { get; set; } = false;

        private class PostedEvent
        {
            public string EventName;
            public string Type; // null, "boolean", "queue"
            public Action Callback;
            public Dictionary<string, object> Kwargs;
        }

        private void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);
        }

        /// <summary>
        /// Add an event handler
        /// </summary>
        /// <param name="eventName">Event name to register for</param>
        /// <param name="handler">Handler callback</param>
        /// <param name="priority">Priority (higher = called first)</param>
        /// <param name="kwargs">Additional kwargs to attach to handler</param>
        /// <param name="condition">Optional condition function</param>
        /// <param name="blockingFacility">Optional blocking facility</param>
        /// <returns>EventHandlerKey for later removal</returns>
        public EventHandlerKey AddHandler(
            string eventName,
            Action<Dictionary<string, object>> handler,
            int priority = 1,
            Dictionary<string, object> kwargs = null,
            Func<bool> condition = null,
            object blockingFacility = null)
        {
            if (string.IsNullOrEmpty(eventName))
            {
                throw new ArgumentException("Event name cannot be null or empty");
            }

            if (handler == null)
            {
                throw new ArgumentException("Handler cannot be null");
            }

            // Parse event name for conditions and priority modifiers
            (string parsedEvent, Func<bool> parsedCondition, int additionalPriority) =
                ParseEventString(eventName);

            priority += additionalPriority;
            if (condition == null)
            {
                condition = parsedCondition;
            }

            Guid key = Guid.NewGuid();

            // Create registered handler
            var registeredHandler = new RegisteredHandler(
                handler, priority, kwargs, key, condition, blockingFacility);

            // Add to handlers list
            if (!registeredHandlers.ContainsKey(parsedEvent))
            {
                registeredHandlers[parsedEvent] = new List<RegisteredHandler>();
            }

            registeredHandlers[parsedEvent].Add(registeredHandler);

            // Sort by priority (descending)
            if (registeredHandlers[parsedEvent].Count > 1)
            {
                registeredHandlers[parsedEvent] = registeredHandlers[parsedEvent]
                    .OrderByDescending(h => h.Priority)
                    .ToList();
            }

            if (DebugMode)
            {
                Debug.Log($"[EventManager] Registered handler for '{parsedEvent}', priority: {priority}");
            }

            return new EventHandlerKey(key, parsedEvent);
        }

        /// <summary>
        /// Parse event string to extract event name, condition, and priority
        /// Format: "event_name.priority{condition}"
        /// </summary>
        private (string eventName, Func<bool> condition, int additionalPriority) ParseEventString(string eventString)
        {
            Func<bool> condition = null;
            int additionalPriority = 0;
            string eventName = eventString;

            // Parse condition in braces
            if (eventString.EndsWith("}"))
            {
                int firstBracket = eventString.IndexOf('{');
                if (firstBracket < 0)
                {
                    throw new FormatException($"Failed to parse condition in event name: {eventString}");
                }

                if (eventString.Substring(0, firstBracket).Contains(" "))
                {
                    throw new FormatException($"Cannot handle events with spaces in the event name: {eventString}");
                }

                // Extract condition string (would need placeholder manager to evaluate)
                string conditionStr = eventString.Substring(firstBracket + 1, eventString.Length - firstBracket - 2);
                // TODO: Implement placeholder manager integration
                // For now, just remove the condition
                eventName = eventString.Substring(0, firstBracket);
            }
            else
            {
                if (eventString.Contains(" "))
                {
                    throw new FormatException($"Cannot handle events with spaces in the event name: {eventString}");
                }
                if (eventString.Contains("{"))
                {
                    throw new FormatException($"Failed to parse condition in event name: {eventString}");
                }
            }

            // Parse priority modifier
            int priorityStart = eventName.IndexOf('.');
            if (priorityStart > 0)
            {
                string priorityStr = eventName.Substring(priorityStart + 1);
                if (!int.TryParse(priorityStr, out additionalPriority))
                {
                    throw new FormatException($"Failed to parse priority in event name: {eventName}");
                }
                eventName = eventName.Substring(0, priorityStart);
            }

            return (eventName, condition, additionalPriority);
        }

        /// <summary>
        /// Remove handler by key
        /// </summary>
        public void RemoveHandlerByKey(EventHandlerKey key)
        {
            if (!registeredHandlers.ContainsKey(key.Event))
            {
                return;
            }

            registeredHandlers[key.Event].RemoveAll(h => h.Key == key.Key);

            if (registeredHandlers[key.Event].Count == 0)
            {
                registeredHandlers.Remove(key.Event);
                if (DebugMode)
                {
                    Debug.Log($"[EventManager] Removed event '{key.Event}' - no more handlers");
                }
            }
        }

        /// <summary>
        /// Remove all handlers for a specific method
        /// </summary>
        public void RemoveHandler(Action<Dictionary<string, object>> method)
        {
            var eventsToRemove = new List<string>();

            foreach (var kvp in registeredHandlers)
            {
                kvp.Value.RemoveAll(h => h.Callback == method);
                if (kvp.Value.Count == 0)
                {
                    eventsToRemove.Add(kvp.Key);
                }
            }

            foreach (var evt in eventsToRemove)
            {
                registeredHandlers.Remove(evt);
            }
        }

        /// <summary>
        /// Remove handler by event and method
        /// </summary>
        public void RemoveHandlerByEvent(string eventName, Action<Dictionary<string, object>> handler)
        {
            if (!registeredHandlers.ContainsKey(eventName))
            {
                return;
            }

            registeredHandlers[eventName].RemoveAll(h => h.Callback == handler);

            if (registeredHandlers[eventName].Count == 0)
            {
                registeredHandlers.Remove(eventName);
            }
        }

        /// <summary>
        /// Post an event - standard event processing
        /// </summary>
        public void Post(string eventName, Action callback = null, Dictionary<string, object> kwargs = null)
        {
            PostInternal(eventName, null, callback, kwargs);
        }

        /// <summary>
        /// Post a boolean event - stops processing if any handler returns false
        /// </summary>
        public void PostBoolean(string eventName, Action callback = null, Dictionary<string, object> kwargs = null)
        {
            PostInternal(eventName, "boolean", callback, kwargs);
        }

        /// <summary>
        /// Post a queue event - waits for all handlers to complete
        /// </summary>
        public void PostQueue(string eventName, Action callback, Dictionary<string, object> kwargs = null)
        {
            PostInternal(eventName, "queue", callback, kwargs);
        }

        /// <summary>
        /// Internal post method
        /// </summary>
        private void PostInternal(string eventName, string type, Action callback, Dictionary<string, object> kwargs)
        {
            var postedEvent = new PostedEvent
            {
                EventName = eventName,
                Type = type,
                Callback = callback,
                Kwargs = kwargs ?? new Dictionary<string, object>()
            };

            eventQueue.Enqueue(postedEvent);

            if (MonitorEvents)
            {
                Debug.Log($"[EventManager] Posted event: {eventName}");
            }

            // Process queue if not already processing
            if (!processingEvents)
            {
                ProcessEventQueue();
            }
        }

        /// <summary>
        /// Process event queue serially
        /// </summary>
        private void ProcessEventQueue()
        {
            processingEvents = true;

            while (eventQueue.Count > 0)
            {
                var evt = eventQueue.Dequeue();
                ProcessEvent(evt);
            }

            processingEvents = false;
        }

        /// <summary>
        /// Process a single event
        /// </summary>
        private void ProcessEvent(PostedEvent evt)
        {
            if (!registeredHandlers.ContainsKey(evt.EventName))
            {
                // No handlers registered
                evt.Callback?.Invoke();
                return;
            }

            var handlers = registeredHandlers[evt.EventName];

            if (MonitorEvents)
            {
                Debug.Log($"[EventManager] Processing event '{evt.EventName}' with {handlers.Count} handlers");
            }

            bool eventResult = true;

            foreach (var handler in handlers)
            {
                // Check condition if present
                if (handler.Condition != null && !handler.Condition())
                {
                    continue;
                }

                // Merge kwargs (event kwargs override handler kwargs)
                var mergedKwargs = new Dictionary<string, object>(handler.Kwargs);
                foreach (var kvp in evt.Kwargs)
                {
                    mergedKwargs[kvp.Key] = kvp.Value;
                }

                try
                {
                    // Call handler
                    handler.Callback(mergedKwargs);

                    // For boolean events, check if we should continue
                    if (evt.Type == "boolean" && mergedKwargs.ContainsKey("ev_result"))
                    {
                        if (mergedKwargs["ev_result"] is bool result && !result)
                        {
                            eventResult = false;
                            break;
                        }
                    }
                }
                catch (Exception ex)
                {
                    Debug.LogError($"[EventManager] Error in handler for event '{evt.EventName}': {ex}");
                }
            }

            // Call completion callback
            if (evt.Callback != null)
            {
                if (evt.Type == "boolean" && !eventResult)
                {
                    // Add result to callback kwargs (if we had a way to pass it)
                    // For now, just invoke
                }
                evt.Callback();
            }
        }

        /// <summary>
        /// Get count of registered handlers for an event
        /// </summary>
        public int GetHandlerCount(string eventName)
        {
            if (!registeredHandlers.ContainsKey(eventName))
            {
                return 0;
            }
            return registeredHandlers[eventName].Count;
        }

        /// <summary>
        /// Check if event has handlers
        /// </summary>
        public bool HasHandlers(string eventName)
        {
            return registeredHandlers.ContainsKey(eventName) && registeredHandlers[eventName].Count > 0;
        }

        /// <summary>
        /// Debug dump of all registered events
        /// </summary>
        public void DebugDumpEvents()
        {
            Debug.Log("=== DEBUG DUMP EVENTS ===");
            Debug.Log($"Total registered events: {registeredHandlers.Count}");
            Debug.Log($"Event queue length: {eventQueue.Count}");

            var sortedEvents = registeredHandlers.OrderByDescending(kvp => kvp.Value.Count);
            foreach (var kvp in sortedEvents)
            {
                Debug.Log($"  Event: '{kvp.Key}' - {kvp.Value.Count} handlers");
                foreach (var handler in kvp.Value)
                {
                    Debug.Log($"    Priority: {handler.Priority}, Method: {handler.Callback.Method.Name}");
                }
            }
            Debug.Log("=== END DEBUG DUMP ===");
        }
    }
}
