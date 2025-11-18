using System;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Devices
{
    /// <summary>
    /// Switch device - represents a physical or virtual switch in a pinball machine
    /// Converted from Python mpf/devices/switch.py
    /// </summary>
    public class Switch : SystemWideDevice
    {
        public new static string ConfigSection = "switches";
        public new static string Collection = "switches";
        public new static string ClassLabel = "switch";

        // Hardware switch interface (will be platform-specific)
        public object HwSwitch { get; set; }

        // Logical state (considers NC/NO configuration)
        private int _state = 0;
        public int State
        {
            get => _state;
            private set
            {
                if (_state != value)
                {
                    int oldState = _state;
                    _state = value;
                    OnStateChanged(oldState, value);
                }
            }
        }

        // Hardware state (raw hardware reading)
        public int HwState { get; private set; } = 0;

        // Invert state (for NC switches)
        public bool Invert { get; private set; } = false;

        // Debouncing
        public float DebounceMsOpen { get; private set; } = 30f;
        public float DebounceMsClose { get; private set; } = 30f;

        // Last change time
        public float LastChange { get; private set; } = -100000f;

        // Recycle protection
        public float RecycleSecs { get; private set; } = 0f;
        private float recycleClearTime = 0f;
        public int RecycleJitterCount { get; private set; } = 0;

        // Events to post on state changes
        private Dictionary<int, List<string>> eventsToPost = new Dictionary<int, List<string>>
        {
            { 0, new List<string>() },
            { 1, new List<string>() }
        };

        protected override void DeviceInitialize()
        {
            base.DeviceInitialize();

            // Extract config
            if (Config.ContainsKey("type"))
            {
                string type = Config["type"].ToString().ToLower();
                Invert = (type == "nc"); // Normally Closed switches are inverted
            }

            if (Config.ContainsKey("debounce_open"))
            {
                DebounceMsOpen = Convert.ToSingle(Config["debounce_open"]);
            }

            if (Config.ContainsKey("debounce_close"))
            {
                DebounceMsClose = Convert.ToSingle(Config["debounce_close"]);
            }

            if (Config.ContainsKey("recycle_secs"))
            {
                RecycleSecs = Convert.ToSingle(Config["recycle_secs"]);
            }

            // Set up activation events
            SetupActivationEvents();

            // Register with switch controller
            // machine.SwitchController?.RegisterSwitch(this);

            if (DebugMode)
            {
                Debug.Log($"[Switch] Initialized: {DeviceName}, Type: {(Invert ? "NC" : "NO")}");
            }
        }

        private void SetupActivationEvents()
        {
            // Default events
            eventsToPost[1].Add($"{DeviceName}_active");
            eventsToPost[0].Add($"{DeviceName}_inactive");

            // Check for custom activation events in config
            if (Config.ContainsKey("events_when_activated"))
            {
                var events = Config["events_when_activated"];
                if (events is List<string> eventList)
                {
                    eventsToPost[1].AddRange(eventList);
                }
                else if (events is string eventStr)
                {
                    eventsToPost[1].Add(eventStr);
                }
            }

            if (Config.ContainsKey("events_when_deactivated"))
            {
                var events = Config["events_when_deactivated"];
                if (events is List<string> eventList)
                {
                    eventsToPost[0].AddRange(eventList);
                }
                else if (events is string eventStr)
                {
                    eventsToPost[0].Add(eventStr);
                }
            }
        }

        /// <summary>
        /// Update switch state from hardware
        /// </summary>
        public void UpdateState(int hwState, float currentTime = -1)
        {
            if (currentTime < 0)
            {
                currentTime = Time.time;
            }

            // Update hardware state
            HwState = hwState;

            // Calculate logical state (consider inversion)
            int logicalState = Invert ? (1 - hwState) : hwState;

            // Check recycle protection
            if (RecycleSecs > 0 && logicalState == 1)
            {
                if (currentTime < recycleClearTime)
                {
                    RecycleJitterCount++;
                    if (DebugMode)
                    {
                        Debug.LogWarning($"[Switch] {DeviceName} recycle jitter detected. Count: {RecycleJitterCount}");
                    }
                    return; // Ignore this activation
                }
                recycleClearTime = currentTime + RecycleSecs;
            }

            // Update state if changed
            if (State != logicalState)
            {
                LastChange = currentTime;
                State = logicalState;
            }
        }

        /// <summary>
        /// Called when state changes
        /// </summary>
        private void OnStateChanged(int oldState, int newState)
        {
            if (DebugMode)
            {
                Debug.Log($"[Switch] {DeviceName} state changed: {oldState} -> {newState}");
            }

            // Post events for this state
            if (eventsToPost.ContainsKey(newState))
            {
                foreach (var eventName in eventsToPost[newState])
                {
                    machine.Events?.Post(eventName, null, new Dictionary<string, object>
                    {
                        { "state", newState },
                        { "ms", GetMsSinceLastChange() }
                    });
                }
            }

            // Notify device manager for BCP
            if (machine.DeviceManager != null)
            {
                machine.DeviceManager.NotifyDeviceChanges(this, "state", oldState, newState);
            }
        }

        /// <summary>
        /// Get milliseconds since last state change
        /// </summary>
        public float GetMsSinceLastChange(float currentTime = -1)
        {
            if (currentTime < 0)
            {
                currentTime = Time.time;
            }
            return (currentTime - LastChange) * 1000f;
        }

        /// <summary>
        /// Check if switch is active
        /// </summary>
        public bool IsActive()
        {
            return State == 1;
        }

        /// <summary>
        /// Check if switch is inactive
        /// </summary>
        public bool IsInactive()
        {
            return State == 0;
        }

        /// <summary>
        /// Manually activate switch (for testing/virtual hardware)
        /// </summary>
        public void Activate()
        {
            UpdateState(Invert ? 0 : 1);
        }

        /// <summary>
        /// Manually deactivate switch (for testing/virtual hardware)
        /// </summary>
        public void Deactivate()
        {
            UpdateState(Invert ? 1 : 0);
        }

        /// <summary>
        /// Toggle switch state (for testing)
        /// </summary>
        public void Toggle()
        {
            if (IsActive())
            {
                Deactivate();
            }
            else
            {
                Activate();
            }
        }
    }
}
