using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Devices
{
    /// <summary>
    /// Light color stack entry
    /// </summary>
    public class LightStackEntry
    {
        public int Priority { get; set; }
        public Color Color { get; set; }
        public float FadeMs { get; set; }
        public string Key { get; set; }

        public LightStackEntry(int priority, Color color, float fadeMs, string key = null)
        {
            Priority = priority;
            Color = color;
            FadeMs = fadeMs;
            Key = key ?? Guid.NewGuid().ToString();
        }
    }

    /// <summary>
    /// Light device - represents an RGB/RGBW light in a pinball machine
    /// Converted from Python mpf/devices/light.py
    /// Supports color changes, fading, and priority-based stacking
    /// </summary>
    public class Light : SystemWideDevice
    {
        public new static string ConfigSection = "lights";
        public new static string Collection = "lights";
        public new static string ClassLabel = "light";

        // Hardware light interface (platform-specific)
        public object HwLight { get; set; }

        // Light type (rgb, rgbw, single color, etc.)
        public string LightType { get; private set; } = "rgb";

        // Current color
        private Color _currentColor = Color.black;
        public Color CurrentColor
        {
            get => _currentColor;
            private set
            {
                if (_currentColor != value)
                {
                    Color oldColor = _currentColor;
                    _currentColor = value;
                    OnColorChanged(oldColor, value);
                }
            }
        }

        // Priority stack for color commands
        private List<LightStackEntry> colorStack = new List<LightStackEntry>();

        // Default fade time
        public float DefaultFadeMs { get; private set; } = 0f;

        // Current fade coroutine
        private Coroutine fadeCoroutine;
        private Color fadeStartColor;
        private Color fadeTargetColor;
        private float fadeStartTime;
        private float fadeDuration;

        // Color correction (optional)
        public Color ColorCorrection { get; set; } = Color.white;

        protected override void DeviceInitialize()
        {
            base.DeviceInitialize();

            // Extract config
            if (Config.ContainsKey("type"))
            {
                LightType = Config["type"].ToString();
            }

            if (Config.ContainsKey("default_fade_ms"))
            {
                DefaultFadeMs = Convert.ToSingle(Config["default_fade_ms"]);
            }

            if (Config.ContainsKey("color_correction"))
            {
                // Parse color correction (would be more complex in full implementation)
                ColorCorrection = Color.white;
            }

            if (DebugMode)
            {
                Debug.Log($"[Light] Initialized: {DeviceName}, Type: {LightType}");
            }
        }

        /// <summary>
        /// Set light color with priority and fade
        /// </summary>
        public void SetColor(Color color, int priority = 0, float? fadeMs = null, string key = null)
        {
            float fade = fadeMs ?? DefaultFadeMs;

            // Add or update entry in stack
            var entry = new LightStackEntry(priority, color, fade, key);

            // Remove existing entry with same key if present
            if (!string.IsNullOrEmpty(key))
            {
                colorStack.RemoveAll(e => e.Key == key);
            }

            colorStack.Add(entry);

            // Sort by priority (descending)
            colorStack.Sort((a, b) => b.Priority.CompareTo(a.Priority));

            // Update to highest priority color
            UpdateFromStack();

            if (DebugMode)
            {
                Debug.Log($"[Light] {DeviceName} set color: {color} at priority {priority}, fade: {fade}ms");
            }
        }

        /// <summary>
        /// Set color from hex string
        /// </summary>
        public void SetColorHex(string hexColor, int priority = 0, float? fadeMs = null, string key = null)
        {
            if (ColorUtility.TryParseHtmlString(hexColor, out Color color))
            {
                SetColor(color, priority, fadeMs, key);
            }
            else
            {
                Debug.LogError($"[Light] {DeviceName} invalid hex color: {hexColor}");
            }
        }

        /// <summary>
        /// Remove a color stack entry by key
        /// </summary>
        public void RemoveColorByKey(string key)
        {
            if (string.IsNullOrEmpty(key))
            {
                return;
            }

            int removed = colorStack.RemoveAll(e => e.Key == key);
            if (removed > 0)
            {
                UpdateFromStack();
            }
        }

        /// <summary>
        /// Clear all colors at or below a priority level
        /// </summary>
        public void ClearStack(int? maxPriority = null)
        {
            if (maxPriority.HasValue)
            {
                colorStack.RemoveAll(e => e.Priority <= maxPriority.Value);
            }
            else
            {
                colorStack.Clear();
            }

            UpdateFromStack();
        }

        /// <summary>
        /// Update light to highest priority color in stack
        /// </summary>
        private void UpdateFromStack()
        {
            if (colorStack.Count == 0)
            {
                // No colors in stack, turn off
                ApplyColor(Color.black, 0f);
                return;
            }

            // Get highest priority entry
            var topEntry = colorStack[0];
            ApplyColor(topEntry.Color, topEntry.FadeMs);
        }

        /// <summary>
        /// Apply a color to the light with fade
        /// </summary>
        private void ApplyColor(Color targetColor, float fadeMs)
        {
            // Apply color correction
            Color correctedColor = new Color(
                targetColor.r * ColorCorrection.r,
                targetColor.g * ColorCorrection.g,
                targetColor.b * ColorCorrection.b,
                targetColor.a * ColorCorrection.a
            );

            if (fadeMs <= 0)
            {
                // Immediate change
                if (fadeCoroutine != null)
                {
                    StopCoroutine(fadeCoroutine);
                    fadeCoroutine = null;
                }
                CurrentColor = correctedColor;
            }
            else
            {
                // Fade to color
                StartFade(correctedColor, fadeMs);
            }
        }

        /// <summary>
        /// Start a color fade
        /// </summary>
        private void StartFade(Color targetColor, float fadeMs)
        {
            if (fadeCoroutine != null)
            {
                StopCoroutine(fadeCoroutine);
            }

            fadeStartColor = CurrentColor;
            fadeTargetColor = targetColor;
            fadeStartTime = Time.time;
            fadeDuration = fadeMs / 1000f;

            fadeCoroutine = StartCoroutine(FadeCoroutine());
        }

        /// <summary>
        /// Fade coroutine
        /// </summary>
        private IEnumerator FadeCoroutine()
        {
            while (Time.time < fadeStartTime + fadeDuration)
            {
                float t = (Time.time - fadeStartTime) / fadeDuration;
                CurrentColor = Color.Lerp(fadeStartColor, fadeTargetColor, t);
                yield return null;
            }

            // Ensure we hit the exact target
            CurrentColor = fadeTargetColor;
            fadeCoroutine = null;
        }

        /// <summary>
        /// Called when color changes
        /// </summary>
        private void OnColorChanged(Color oldColor, Color newColor)
        {
            if (DebugMode)
            {
                Debug.Log($"[Light] {DeviceName} color changed: {oldColor} -> {newColor}");
            }

            // Post event
            machine.Events?.Post($"{DeviceName}_color_changed", null, new Dictionary<string, object>
            {
                { "color", newColor }
            });

            // Update hardware (in real implementation)
            // HwLight?.SetColor(newColor);

            // Notify device manager for BCP
            machine.DeviceManager?.NotifyDeviceChanges(this, "color", oldColor, newColor);
        }

        /// <summary>
        /// Turn light on to a color
        /// </summary>
        public void On(Color? color = null, int priority = 0, float? fadeMs = null, string key = null)
        {
            Color c = color ?? Color.white;
            SetColor(c, priority, fadeMs, key);
        }

        /// <summary>
        /// Turn light off
        /// </summary>
        public void Off(int priority = 0, float? fadeMs = null, string key = null)
        {
            SetColor(Color.black, priority, fadeMs, key);
        }

        /// <summary>
        /// Toggle light (on/off)
        /// </summary>
        public void Toggle(Color? onColor = null)
        {
            if (CurrentColor == Color.black || CurrentColor.a == 0)
            {
                On(onColor);
            }
            else
            {
                Off();
            }
        }

        /// <summary>
        /// Get current brightness (0-1)
        /// </summary>
        public float GetBrightness()
        {
            return CurrentColor.maxColorComponent;
        }

        public override void StopDevice()
        {
            base.StopDevice();
            Off();
            if (fadeCoroutine != null)
            {
                StopCoroutine(fadeCoroutine);
            }
        }
    }
}
