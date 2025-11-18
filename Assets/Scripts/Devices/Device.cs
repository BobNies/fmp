using System;
using System.Collections.Generic;
using UnityEngine;

namespace MPF.Devices
{
    /// <summary>
    /// Base class for all MPF devices
    /// Converted from Python mpf/core/device.py
    /// </summary>
    public abstract class Device : MonoBehaviour
    {
        // String of the config section name (override in derived classes)
        public static string ConfigSection { get; protected set; }

        // String name of the collection (override in derived classes)
        public static string Collection { get; protected set; }

        // Friendly name of the device class
        public static string ClassLabel { get; protected set; }

        // Can a config for this device be empty?
        public static bool AllowEmptyConfigs { get; protected set; } = false;

        // Reference to machine controller
        protected MPF.Core.MachineController machine;

        // Device name
        public string DeviceName { get; protected set; }

        // Tags applied to this device
        public List<string> Tags { get; protected set; } = new List<string>();

        // Platform reference
        public object Platform { get; set; }

        // Label (friendly name)
        public string Label { get; set; }

        // Configuration dictionary
        public Dictionary<string, object> Config { get; protected set; } = new Dictionary<string, object>();

        // Debug mode
        public bool DebugMode { get; set; } = false;

        /// <summary>
        /// Initialize the device
        /// </summary>
        /// <param name="machineController">Reference to machine controller</param>
        /// <param name="name">Name of the device</param>
        public virtual void Initialize(MPF.Core.MachineController machineController, string name)
        {
            machine = machineController;
            DeviceName = name;
            gameObject.name = $"{GetType().Name}_{name}";

            if (DebugMode)
            {
                Debug.Log($"[Device] Initialized {GetType().Name}: {name}");
            }
        }

        /// <summary>
        /// Configure the device from a config dictionary
        /// </summary>
        public virtual void Configure(Dictionary<string, object> config)
        {
            Config = config ?? new Dictionary<string, object>();

            // Extract common config items
            if (Config.ContainsKey("label"))
            {
                Label = Config["label"].ToString();
            }

            if (Config.ContainsKey("tags"))
            {
                var tags = Config["tags"];
                if (tags is List<string> tagList)
                {
                    Tags = tagList;
                }
                else if (tags is string tagString)
                {
                    Tags = new List<string> { tagString };
                }
            }

            if (Config.ContainsKey("platform"))
            {
                // Platform loading happens in derived classes
            }
        }

        /// <summary>
        /// Validate and parse config
        /// Override in derived classes for specific validation
        /// </summary>
        public virtual Dictionary<string, object> ValidateAndParseConfig(Dictionary<string, object> config, bool isModeConfig)
        {
            // Base implementation just returns the config
            // Derived classes should override for validation
            return config;
        }

        /// <summary>
        /// Prepare config before validation
        /// </summary>
        public virtual Dictionary<string, object> PrepareConfig(Dictionary<string, object> config, bool isModeConfig)
        {
            // Base implementation - override in derived classes if needed
            return config;
        }

        /// <summary>
        /// Called when device is added to a mode
        /// </summary>
        public virtual void DeviceAddedToMode(object mode)
        {
            throw new InvalidOperationException($"Cannot use device {DeviceName} in mode");
        }

        /// <summary>
        /// Load platform section
        /// </summary>
        protected virtual void LoadPlatformSection(string platformSection)
        {
            // Platform loading - to be implemented when platforms are ready
            // this.Platform = machine.GetPlatformSections(platformSection, Config["platform"]);
        }

        /// <summary>
        /// Device-specific initialization (override in derived classes)
        /// </summary>
        protected virtual void DeviceInitialize()
        {
            // Override in derived classes
        }

        /// <summary>
        /// Stop the device
        /// </summary>
        public virtual void StopDevice()
        {
            if (DebugMode)
            {
                Debug.Log($"[Device] Stopping device: {DeviceName}");
            }
        }

        /// <summary>
        /// Compare devices by name
        /// </summary>
        public int CompareTo(Device other)
        {
            return string.Compare(DeviceName, other.DeviceName, StringComparison.Ordinal);
        }

        protected virtual void OnDestroy()
        {
            StopDevice();
        }
    }

    /// <summary>
    /// Base class for system-wide devices (not mode-specific)
    /// </summary>
    public abstract class SystemWideDevice : Device
    {
        // System-wide devices exist for the lifetime of the machine
    }

    /// <summary>
    /// Base class for mode-specific devices
    /// </summary>
    public abstract class ModeDevice : Device
    {
        // Mode devices are created/destroyed with modes
        public object Mode { get; set; }

        public override void DeviceAddedToMode(object mode)
        {
            Mode = mode;
        }
    }
}
