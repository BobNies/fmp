using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using MPF.Devices;

namespace MPF.Core
{
    /// <summary>
    /// Device collection - dictionary of devices by name
    /// </summary>
    public class DeviceCollection<T> : Dictionary<string, T> where T : Device
    {
        public string CollectionName { get; private set; }

        public DeviceCollection(string collectionName)
        {
            CollectionName = collectionName;
        }
    }

    /// <summary>
    /// Device Manager - manages all devices in the machine
    /// Converted from Python mpf/core/device_manager.py
    /// </summary>
    public class DeviceManager : MonoBehaviour
    {
        private static DeviceManager _instance;
        public static DeviceManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    var go = new GameObject("DeviceManager");
                    _instance = go.AddComponent<DeviceManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        private MachineController machine;

        // Dictionary of all device collections
        public Dictionary<string, object> Collections { get; private set; } = new Dictionary<string, object>();

        // Dictionary of device classes (type) by collection name
        private Dictionary<string, Type> deviceClasses = new Dictionary<string, Type>();

        // Monitorable devices for BCP
        private Dictionary<string, Dictionary<string, Device>> monitorableDevices =
            new Dictionary<string, Dictionary<string, Device>>();

        public bool DebugMode { get; set; } = false;

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
        /// Initialize the device manager
        /// </summary>
        public void Initialize(MachineController machineController)
        {
            machine = machineController;

            if (DebugMode)
            {
                Debug.Log("[DeviceManager] Initializing...");
            }

            // Register for initialization events
            machine.Events.AddHandler("init_phase_1", LoadDeviceModules, priority: 5);
            machine.Events.AddHandler("init_phase_2", CreateMachinewideDeviceControlEvents, priority: 2);
        }

        /// <summary>
        /// Load device modules (create devices)
        /// </summary>
        private void LoadDeviceModules(Dictionary<string, object> kwargs)
        {
            if (DebugMode)
            {
                Debug.Log("[DeviceManager] Creating devices...");
            }

            // In a full implementation, this would read from config
            // For now, we'll create device collections manually

            // Example: Create switch collection
            // CreateDeviceCollection<Switch>("switches", switchConfig);

            // This will be populated when we have actual config loading
        }

        /// <summary>
        /// Create a device collection
        /// </summary>
        public DeviceCollection<T> CreateDeviceCollection<T>(string collectionName, Dictionary<string, Dictionary<string, object>> config = null)
            where T : Device
        {
            var collection = new DeviceCollection<T>(collectionName);
            Collections[collectionName] = collection;

            if (config != null)
            {
                CreateDevices<T>(collection, config);
            }

            if (DebugMode)
            {
                Debug.Log($"[DeviceManager] Created device collection: {collectionName}");
            }

            return collection;
        }

        /// <summary>
        /// Create devices from config
        /// </summary>
        private void CreateDevices<T>(DeviceCollection<T> collection, Dictionary<string, Dictionary<string, object>> config)
            where T : Device
        {
            foreach (var kvp in config)
            {
                string deviceName = kvp.Key;
                var deviceConfig = kvp.Value;

                // Create device GameObject
                var deviceGO = new GameObject($"{typeof(T).Name}_{deviceName}");
                deviceGO.transform.SetParent(transform);

                // Add component
                var device = deviceGO.AddComponent<T>();
                device.Initialize(machine, deviceName);
                device.Configure(deviceConfig);

                // Add to collection
                collection[deviceName] = device;

                if (DebugMode)
                {
                    Debug.Log($"[DeviceManager] Created device: {typeof(T).Name}.{deviceName}");
                }
            }
        }

        /// <summary>
        /// Initialize all devices
        /// </summary>
        public IEnumerator InitializeDevices()
        {
            if (DebugMode)
            {
                Debug.Log("[DeviceManager] Initializing devices...");
            }

            foreach (var collection in Collections.Values)
            {
                if (collection is IDictionary dict)
                {
                    foreach (var device in dict.Values)
                    {
                        if (device is Device d)
                        {
                            // Device-specific initialization
                            d.SendMessage("DeviceInitialize", SendMessageOptions.DontRequireReceiver);
                        }
                    }
                }
            }

            yield return null;
        }

        /// <summary>
        /// Stop all devices
        /// </summary>
        public void StopDevices()
        {
            foreach (var collection in Collections.Values)
            {
                if (collection is IDictionary dict)
                {
                    foreach (var device in dict.Values)
                    {
                        if (device is Device d)
                        {
                            d.StopDevice();
                        }
                    }
                }
            }
        }

        /// <summary>
        /// Create machine-wide device control events
        /// </summary>
        private void CreateMachinewideDeviceControlEvents(Dictionary<string, object> kwargs)
        {
            // This would create control events for all devices
            // e.g., "switch_<name>_active", "coil_<name>_pulse", etc.
            if (DebugMode)
            {
                Debug.Log("[DeviceManager] Creating machine-wide device control events");
            }
        }

        /// <summary>
        /// Register a monitorable device for BCP
        /// </summary>
        public void RegisterMonitorableDevice(Device device, string collectionName)
        {
            if (!monitorableDevices.ContainsKey(collectionName))
            {
                monitorableDevices[collectionName] = new Dictionary<string, Device>();
            }
            monitorableDevices[collectionName][device.DeviceName] = device;
        }

        /// <summary>
        /// Get monitorable devices
        /// </summary>
        public Dictionary<string, Dictionary<string, Device>> GetMonitorableDevices()
        {
            return monitorableDevices;
        }

        /// <summary>
        /// Notify subscribers about device changes (for BCP)
        /// </summary>
        public void NotifyDeviceChanges(Device device, string attributeName, object oldValue, object newValue)
        {
            // When BCP is implemented, this will notify clients
            if (DebugMode)
            {
                Debug.Log($"[DeviceManager] Device {device.DeviceName}.{attributeName} changed: {oldValue} -> {newValue}");
            }
        }

        /// <summary>
        /// Get device by name from a collection
        /// </summary>
        public T GetDevice<T>(string collectionName, string deviceName) where T : Device
        {
            if (!Collections.ContainsKey(collectionName))
            {
                return null;
            }

            if (Collections[collectionName] is DeviceCollection<T> collection)
            {
                if (collection.ContainsKey(deviceName))
                {
                    return collection[deviceName];
                }
            }

            return null;
        }

        private void OnDestroy()
        {
            StopDevices();
            if (_instance == this)
            {
                _instance = null;
            }
        }
    }
}
