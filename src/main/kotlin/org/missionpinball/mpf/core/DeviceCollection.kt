package org.missionpinball.mpf.core

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * A collection of Devices.
 *
 * One instance of this class will be created for each different type of
 * hardware device (such as coils, lights, switches, ball devices, etc.).
 *
 * This class extends MutableMap to provide dictionary-like access to devices.
 */
class DeviceCollection<T : Device>(
    val machine: MachineController,
    val name: String,
    val configSection: String
) : MutableMap<String, T> by mutableMapOf() {

    private val delegate: MutableMap<String, T> = mutableMapOf()
    private var tagCache: MutableMap<String, List<T>> = mutableMapOf()

    /**
     * Get a device by name.
     */
    override fun get(key: String): T? = delegate[key]

    /**
     * Add or update a device.
     */
    override fun put(key: String, value: T): T? {
        tagCache.clear()  // Clear tag cache when collection changes
        return delegate.put(key, value)
    }

    /**
     * Remove a device.
     */
    override fun remove(key: String): T? {
        tagCache.clear()  // Clear tag cache when collection changes
        return delegate.remove(key)
    }

    /**
     * Check if collection contains a device.
     */
    override fun containsKey(key: String): Boolean = delegate.containsKey(key)

    /**
     * Check if collection contains a value.
     */
    override fun containsValue(value: T): Boolean = delegate.containsValue(value)

    /**
     * Get all device names (keys).
     */
    override val keys: MutableSet<String>
        get() = delegate.keys

    /**
     * Get all devices (values).
     */
    override val values: MutableCollection<T>
        get() = delegate.values

    /**
     * Get all entries.
     */
    override val entries: MutableSet<MutableMap.MutableEntry<String, T>>
        get() = delegate.entries

    /**
     * Get the size of the collection.
     */
    override val size: Int
        get() = delegate.size

    /**
     * Check if collection is empty.
     */
    override fun isEmpty(): Boolean = delegate.isEmpty()

    /**
     * Clear all devices from collection.
     */
    override fun clear() {
        tagCache.clear()
        delegate.clear()
    }

    /**
     * Put all entries from another map.
     */
    override fun putAll(from: Map<out String, T>) {
        tagCache.clear()
        delegate.putAll(from)
    }

    /**
     * Return a list of device objects which have a certain tag.
     *
     * @param tag A string of the tag name which specifies what devices are returned.
     *            A value of "*" returns all devices.
     * @return List of device objects. If no devices are found with that tag,
     *         returns an empty list.
     */
    fun itemsTagged(tag: String): List<T> {
        // Return all devices if wildcard
        if (tag == "*") {
            return values.toList()
        }

        // Check cache first
        tagCache[tag]?.let { return it }

        // Build list of devices with this tag
        val items = values.filter { device ->
            tag in device.tags
        }

        // Cache the result
        tagCache[tag] = items

        return items
    }

    /**
     * Get a device by name, throwing an exception if not found.
     *
     * @param deviceName The name of the device to get.
     * @return The device.
     * @throws NoSuchElementException if device doesn't exist.
     */
    fun getOrThrow(deviceName: String): T {
        return delegate[deviceName] ?: throw NoSuchElementException(
            "No device exists with the name: $deviceName in collection: $name"
        )
    }

    override fun hashCode(): Int {
        return name.hashCode() xor machine.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceCollection<*>) return false
        return name == other.name && machine == other.machine
    }

    override fun toString(): String {
        return "DeviceCollection(name='$name', configSection='$configSection', size=$size)"
    }
}
