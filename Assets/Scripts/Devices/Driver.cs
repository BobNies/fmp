using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Devices
{
    /// <summary>
    /// Driver (Coil) device - represents a solenoid/coil in a pinball machine
    /// Converted from Python mpf/devices/driver.py
    /// Can pulse, enable, disable, and control coils with PWM
    /// </summary>
    public class Driver : SystemWideDevice
    {
        public new static string ConfigSection = "coils";
        public new static string Collection = "coils";
        public new static string ClassLabel = "coil";

        // Hardware driver interface (platform-specific)
        public object HwDriver { get; set; }

        // Current state
        public bool IsEnabled { get; private set; } = false;

        // Pulse settings
        public float DefaultPulseMs { get; private set; } = 30f;
        public float DefaultPulsePower { get; private set; } = 1.0f;

        // Hold settings (for enabled coils)
        public float DefaultHoldPower { get; private set; } = 0.375f;

        // Reconfigure (disable after time)
        public bool AllowEnable { get; private set; } = true;
        public float MaxHoldDuration { get; private set; } = 0f;

        // Scheduled events
        private MPF.Core.ScheduledEvent timedEnableEvent;

        // Statistics
        public int PulseCount { get; private set; } = 0;
        public float LastPulseTime { get; private set; } = -1f;

        protected override void DeviceInitialize()
        {
            base.DeviceInitialize();

            // Extract config
            if (Config.ContainsKey("default_pulse_ms"))
            {
                DefaultPulseMs = Convert.ToSingle(Config["default_pulse_ms"]);
            }

            if (Config.ContainsKey("default_pulse_power"))
            {
                DefaultPulsePower = Convert.ToSingle(Config["default_pulse_power"]);
            }

            if (Config.ContainsKey("default_hold_power"))
            {
                DefaultHoldPower = Convert.ToSingle(Config["default_hold_power"]);
            }

            if (Config.ContainsKey("allow_enable"))
            {
                AllowEnable = Convert.ToBoolean(Config["allow_enable"]);
            }

            if (Config.ContainsKey("max_hold_duration"))
            {
                MaxHoldDuration = Convert.ToSingle(Config["max_hold_duration"]);
            }

            if (DebugMode)
            {
                Debug.Log($"[Driver] Initialized: {DeviceName}, Pulse: {DefaultPulseMs}ms, Hold: {DefaultHoldPower}");
            }
        }

        /// <summary>
        /// Pulse the coil for a specified duration
        /// </summary>
        public void Pulse(float? pulseMs = null, float? pulsePower = null)
        {
            float ms = pulseMs ?? DefaultPulseMs;
            float power = pulsePower ?? DefaultPulsePower;

            PulseCount++;
            LastPulseTime = Time.time;

            if (DebugMode)
            {
                Debug.Log($"[Driver] {DeviceName} pulse: {ms}ms at {power * 100}% power");
            }

            // Post event
            machine.Events?.Post($"{DeviceName}_pulsing", null, new Dictionary<string, object>
            {
                { "pulse_ms", ms },
                { "pulse_power", power }
            });

            // In real implementation, this would call platform-specific pulse
            // HwDriver?.Pulse(ms, power);

            // Simulate pulse completion
            machine.Clock?.ScheduleOnce(() =>
            {
                machine.Events?.Post($"{DeviceName}_pulse_complete");
            }, ms / 1000f);
        }

        /// <summary>
        /// Enable the coil (hold on continuously)
        /// </summary>
        public void Enable(float? holdPower = null)
        {
            if (!AllowEnable)
            {
                Debug.LogWarning($"[Driver] {DeviceName} does not allow enable. Ignoring enable request.");
                return;
            }

            float power = holdPower ?? DefaultHoldPower;

            IsEnabled = true;

            if (DebugMode)
            {
                Debug.Log($"[Driver] {DeviceName} enabled at {power * 100}% power");
            }

            // Post event
            machine.Events?.Post($"{DeviceName}_enabling", null, new Dictionary<string, object>
            {
                { "hold_power", power }
            });

            // In real implementation, this would call platform-specific enable
            // HwDriver?.Enable(power);

            // Check max hold duration
            if (MaxHoldDuration > 0)
            {
                timedEnableEvent = machine.Clock?.ScheduleOnce(() =>
                {
                    Debug.LogWarning($"[Driver] {DeviceName} max hold duration ({MaxHoldDuration}s) exceeded. Auto-disabling.");
                    Disable();
                }, MaxHoldDuration);
            }
        }

        /// <summary>
        /// Disable the coil
        /// </summary>
        public void Disable()
        {
            if (!IsEnabled)
            {
                return;
            }

            IsEnabled = false;

            // Cancel timed disable if scheduled
            if (timedEnableEvent != null)
            {
                timedEnableEvent.Cancel();
                timedEnableEvent = null;
            }

            if (DebugMode)
            {
                Debug.Log($"[Driver] {DeviceName} disabled");
            }

            // Post event
            machine.Events?.Post($"{DeviceName}_disabling");

            // In real implementation, this would call platform-specific disable
            // HwDriver?.Disable();
        }

        /// <summary>
        /// Enable coil for a specific duration, then disable
        /// </summary>
        public void TimedEnable(float durationMs, float? holdPower = null)
        {
            float power = holdPower ?? DefaultHoldPower;

            if (!AllowEnable)
            {
                Debug.LogWarning($"[Driver] {DeviceName} does not allow enable. Ignoring timed enable request.");
                return;
            }

            if (DebugMode)
            {
                Debug.Log($"[Driver] {DeviceName} timed enable: {durationMs}ms at {power * 100}% power");
            }

            Enable(power);

            // Schedule disable
            machine.Clock?.ScheduleOnce(() =>
            {
                Disable();
            }, durationMs / 1000f);
        }

        /// <summary>
        /// Pulse with optional enable (pulse-with-hold)
        /// </summary>
        public void PulseWithHold(float? pulseMs = null, float? pulsePower = null, float? holdPower = null)
        {
            float pulse_ms = pulseMs ?? DefaultPulseMs;
            float pulse_power = pulsePower ?? DefaultPulsePower;
            float hold_power = holdPower ?? DefaultHoldPower;

            if (DebugMode)
            {
                Debug.Log($"[Driver] {DeviceName} pulse with hold: {pulse_ms}ms pulse, then hold at {hold_power * 100}%");
            }

            // Pulse first
            Pulse(pulse_ms, pulse_power);

            // Then enable after pulse
            machine.Clock?.ScheduleOnce(() =>
            {
                Enable(hold_power);
            }, pulse_ms / 1000f);
        }

        /// <summary>
        /// Get time since last pulse
        /// </summary>
        public float GetMsSinceLastPulse()
        {
            if (LastPulseTime < 0)
            {
                return -1f;
            }
            return (Time.time - LastPulseTime) * 1000f;
        }

        public override void StopDevice()
        {
            base.StopDevice();
            Disable();
        }
    }
}
