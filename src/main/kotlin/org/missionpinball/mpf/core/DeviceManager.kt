package org.missionpinball.mpf.core

import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Manages all the devices in MPF.
 *
 * The DeviceManager is responsible for:
 * - Loading device modules and creating device instances
 * - Managing device collections
 * - Validating and loading device configurations
 * - Initializing devices
 * - Creating machine-wide control events for devices
 */
class DeviceManager(machine: MachineController) : MpfController(machine) {

    override val configName = "device_manager"

    /**
     * Map of monitorable devices by collection name.
     */
    private val monitorableDevices = mutableMapOf<String, MutableMap<String, Device>>()

    /**
     * Map of all device collections by collection name.
     */
    val collections = mutableMapOf<String, DeviceCollection<*>>()

    /**
     * Cache of device classes by collection name.
     */
    private val deviceClasses = mutableMapOf<String, Class<out Device>>()

    init {
        // Register event handlers for device initialization phases
        // These will be called during machine initialization
        // TODO: Uncomment when machine events are available
        // machine.events.addAsyncHandler("init_phase_1", ::loadDeviceModules, priority = 5)
        // machine.events.addHandler("init_phase_2", ::createMachinewideDeviceControlEvents, priority = 2)
    }

    /**
     * Get all devices which are registered as monitorable.
     */
    fun getMonitorableDevices(): Map<String, Map<String, Device>> {
        return monitorableDevices.toMap()
    }

    /**
     * Register a monitorable device.
     *
     * @param device The device to register.
     */
    fun registerMonitorableDevice(device: Device) {
        val collection = monitorableDevices.getOrPut(device.collection) { mutableMapOf() }
        collection[device.name] = device
    }

    /**
     * Notify subscribers about changes in a registered device.
     *
     * @param device The device that changed.
     * @param notify Attribute name which changed.
     * @param oldValue The old value.
     * @param newValue The new value.
     */
    fun notifyDeviceChanges(device: Device, notify: String, oldValue: Any?, newValue: Any?) {
        // TODO: Implement BCP notification when BCP interface is available
        // machine.bcp.interface.notifyDeviceChanges(device, notify, oldValue, newValue)
        debugLog("Device ${device.name}.$notify changed from $oldValue to $newValue")
    }

    /**
     * Load device modules and create device instances.
     * This is called during init_phase_1.
     */
    private suspend fun loadDeviceModules() {
        debugLog("Creating devices...")

        // Step 1: Create devices in machine collection
        val deviceModules = machine.config["mpf"]?.let { it as? Map<*, *> }
            ?.get("device_modules") as? Map<*, *> ?: emptyMap<String, String>()

        for ((collectionName, _) in deviceModules) {
            val collectionNameStr = collectionName.toString()

            // Create the collection
            val collection = DeviceCollection<Device>(machine, collectionNameStr, collectionNameStr)

            collections[collectionNameStr] = collection
            // TODO: Set collection as attribute on machine when dynamic attributes are supported
            // setattr(machine, collectionNameStr, collection)

            // Get the config section for these devices
            val config = machine.config[collectionNameStr] as? Map<*, *>

            // Create the devices
            if (config != null) {
                createDevices(collectionNameStr, config)
            }
        }

        // TODO: Create mode devices when mode controller is available
        // machine.modeController.createModeDevices()

        // Step 2: Load config and validate devices
        loadDevicesConfig(validate = true)
        // TODO: Load mode devices when mode controller is available
        // machine.modeController.loadModeDevices()

        // Step 3: Initialize devices (mode devices will be initialized when mode starts)
        initializeDevices()
    }

    /**
     * Stop all devices in the machine.
     */
    fun stopDevices() {
        for (collection in collections.values) {
            for (device in collection.values) {
                device.stopDevice()
            }
        }
    }

    /**
     * Create devices for a collection.
     *
     * @param collectionName Name of the collection.
     * @param config Configuration for the devices.
     */
    fun createDevices(collectionName: String, config: Map<*, *>) {
        // Get or load device class
        val deviceClass = deviceClasses.getOrPut(collectionName) {
            val deviceModules = machine.config["mpf"]?.let { it as? Map<*, *> }
                ?.get("device_modules") as? Map<*, *> ?: emptyMap<String, String>()
            val className = deviceModules[collectionName] as? String
                ?: throw IllegalArgumentException("No device module found for collection: $collectionName")

            // TODO: Implement class loading when utility function is available
            // Util.stringToClass(className) as Class<out Device>
            throw NotImplementedError("Device class loading not yet implemented")
        }

        @Suppress("UNCHECKED_CAST")
        val collection = collections[collectionName] as? DeviceCollection<Device>
            ?: throw IllegalStateException("Collection $collectionName not found")

        // Call device class init if it exists
        try {
            Device.deviceClassInit(machine)
        } catch (e: Exception) {
            debugLog("No deviceClassInit for $collectionName or error calling it: ${e.message}")
        }

        // Create the devices
        for ((deviceName, deviceConfig) in config) {
            val deviceNameStr = deviceName.toString()

            if (deviceConfig == null || (deviceConfig is Map<*, *> && deviceConfig.isEmpty())) {
                // TODO: Get allowEmptyConfigs from device class
                val allowEmpty = false  // deviceClass.getDeclaredField("allowEmptyConfigs").getBoolean(null)
                if (!allowEmpty) {
                    raiseConfigError(
                        "Device $collectionName:'$deviceNameStr' has an empty config.",
                        2,
                        "$collectionName.$deviceNameStr"
                    )
                }
            } else if (deviceConfig !is Map<*, *>) {
                raiseConfigError(
                    "Device $collectionName:'$deviceNameStr' does not have a valid config. Expected a dictionary.",
                    3,
                    "$collectionName.$deviceNameStr"
                )
            }

            // TODO: Create device instance when reflection/instantiation is available
            // val device = deviceClass.getDeclaredConstructor(MachineController::class.java, String::class.java)
            //     .newInstance(machine, deviceNameStr)
            // collection[deviceNameStr] = device
        }
    }

    /**
     * Load configuration for all devices.
     *
     * @param validate Whether to validate the configuration.
     */
    fun loadDevicesConfig(validate: Boolean = true) {
        val deviceModules = machine.config["mpf"]?.let { it as? Map<*, *> }
            ?.get("device_modules") as? Map<*, *> ?: return

        if (validate) {
            for ((collectionName, _) in deviceModules) {
                val collectionNameStr = collectionName.toString()
                val configSection = machine.config[collectionNameStr] as? MutableMap<*, *> ?: continue

                @Suppress("UNCHECKED_CAST")
                val collection = collections[collectionNameStr] as? DeviceCollection<Device> ?: continue

                // Validate config for each device
                for ((deviceName, _) in configSection) {
                    val deviceNameStr = deviceName.toString()
                    val device = collection[deviceNameStr] ?: continue

                    @Suppress("UNCHECKED_CAST")
                    var deviceConfig = configSection[deviceName] as? MutableMap<String, Any?> ?: mutableMapOf()

                    deviceConfig = device.prepareConfig(deviceConfig, false).toMutableMap()
                    deviceConfig = device.validateAndParseConfig(deviceConfig, false).toMutableMap()

                    configSection[deviceName] = deviceConfig
                }
            }
        }

        // Load config into devices
        for ((collectionName, _) in deviceModules) {
            val collectionNameStr = collectionName.toString()
            val configSection = machine.config[collectionNameStr] as? Map<*, *> ?: continue

            @Suppress("UNCHECKED_CAST")
            val collection = collections[collectionNameStr] as? DeviceCollection<Device> ?: continue

            // Load config for each device
            for ((deviceName, deviceConfig) in configSection) {
                val deviceNameStr = deviceName.toString()
                val device = collection[deviceNameStr] ?: continue

                @Suppress("UNCHECKED_CAST")
                device.loadConfig(deviceConfig as? Map<String, Any?> ?: emptyMap())
            }
        }
    }

    /**
     * Initialize all devices.
     */
    private suspend fun initializeDevices() {
        val deviceModules = machine.config["mpf"]?.let { it as? Map<*, *> }
            ?.get("device_modules") as? Map<*, *> ?: return

        val jobs = mutableListOf<Deferred<Unit>>()

        for ((collectionName, _) in deviceModules) {
            val collectionNameStr = collectionName.toString()
            val configSection = machine.config[collectionNameStr] as? Map<*, *> ?: continue

            @Suppress("UNCHECKED_CAST")
            val collection = collections[collectionNameStr] as? DeviceCollection<Device> ?: continue

            // Initialize each device asynchronously
            for ((deviceName, _) in configSection) {
                val deviceNameStr = deviceName.toString()
                val device = collection[deviceNameStr] ?: continue

                jobs.add(CoroutineScope(Dispatchers.Default).async {
                    device.deviceAddedSystemWide()
                })
            }
        }

        // Wait for all devices to initialize
        jobs.awaitAll()
    }

    /**
     * Get device control events from config.
     *
     * Yields events, methods, delays, and devices for all the devices and
     * control_events in the config.
     *
     * @param config An MPF config dictionary (machine-wide or mode-specific).
     * @return Sequence of tuples: (event name, callback method, delay ms, device object).
     */
    fun getDeviceControlEvents(config: Map<String, Any?>): Sequence<DeviceControlEvent> = sequence {
        // TODO: Implement when config_validator is available
        // val configSpec = machine.configValidator.getConfigSpec()

        for ((collectionName, collection) in collections) {
            val configSection = config[collection.configSection] as? Map<*, *> ?: continue

            for ((deviceName, settings) in configSection) {
                val settingsMap = settings as? Map<*, *> ?: continue
                val device = collection[deviceName.toString()] ?: continue

                // Find all control events (attributes ending with '_events')
                val controlEvents = settingsMap.keys
                    .filterIsInstance<String>()
                    .filter { it.endsWith("_events") && it != "control_events" }

                for (controlEvent in controlEvents) {
                    val eventSettings = settingsMap[controlEvent] as? Map<*, *> ?: continue

                    // Get the event method name (remove '_events' suffix)
                    val methodName = "event_${controlEvent.removeSuffix("_events")}"

                    // TODO: Get method from device when reflection is available
                    // val method = device::class.java.getMethod(methodName)

                    for ((event, delay) in eventSettings) {
                        yield(
                            DeviceControlEvent(
                                event = event.toString(),
                                method = { /* TODO: call actual method */ },
                                delay = delay as? Int ?: 0,
                                device = device
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Create machine-wide device control events.
     */
    private fun createMachinewideDeviceControlEvents() {
        for (controlEvent in getDeviceControlEvents(machine.config)) {
            if (controlEvent.delay > 0) {
                // TODO: Add handler with delay when events and delay manager are fully integrated
                /*
                machine.events.addHandler(
                    event = controlEvent.event,
                    handler = ::controlEventHandler,
                    callback = controlEvent.method,
                    msDelay = controlEvent.delay,
                    delayMgr = machine.delay
                )
                */
            } else {
                // TODO: Add handler when events are fully integrated
                // machine.events.addHandler(
                //     event = controlEvent.event,
                //     handler = controlEvent.method
                // )
            }
        }
    }

    /**
     * Handler for delayed control events.
     */
    private fun controlEventHandler(
        callback: () -> Unit,
        msDelay: Int,
        delayMgr: DelayManager? = null
    ) {
        debugLog("_control_event_handler: callback: $callback")
        delayMgr?.add(msDelay.toLong(), callback)
    }
}

/**
 * Data class representing a device control event.
 */
data class DeviceControlEvent(
    val event: String,
    val method: () -> Unit,
    val delay: Int,
    val device: Device
)
