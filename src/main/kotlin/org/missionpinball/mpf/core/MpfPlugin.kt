package org.missionpinball.mpf.core

/**
 * Base class for a plugin module.
 *
 * Plugins extend MPF functionality by providing optional features
 * that can be enabled or disabled in the machine configuration.
 */
abstract class MpfPlugin(
    protected val machine: MachineController
) : LogMixin {

    /**
     * Plugin name (defaults to class name).
     */
    val name: String = this::class.simpleName ?: "UnknownPlugin"

    /**
     * Plugin configuration.
     */
    var config: Map<String, Any?>? = null

    /**
     * Config section for this plugin.
     * Override in subclass to specify the config section name.
     */
    open val configSection: String? = null

    init {
        if (configSection != null && configSection !in (machine.config.keys)) {
            machine.log.debug(
                "\"$configSection:\" section not found in machine configuration, " +
                "so the $name will not be used."
            )
        }
    }

    /**
     * Called when the plugin is enabled and loaded into MPF.
     *
     * Override with plugin-specific init behavior.
     */
    open fun initialize() {
        // Override in subclass
    }

    /**
     * If false, this plugin will not be attached to the MPF process.
     *
     * Override with class-specific logic.
     */
    open val isPluginEnabled: Boolean
        get() {
            return if (configSection != null) {
                configSection in machine.config.keys
            } else {
                true
            }
        }
}
