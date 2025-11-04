package org.missionpinball.mpf.core

/**
 * Base class for custom code in a machine.
 *
 * Custom code allows users to write machine-specific logic that can be
 * loaded and executed by MPF. This provides hooks for custom behavior
 * without modifying MPF core.
 */
abstract class CustomCode(
    protected val machine: MachineController,
    val name: String
) : LogMixin {

    /**
     * Delay manager for this custom code.
     */
    protected val delay = DelayManager(machine.clock)

    init {
        configureLogging("CustomCode.$name", "basic", "full", null)
        onLoad()
    }

    override fun toString(): String {
        return "<Scriptlet.$name>"
    }

    /**
     * Automatically called when this custom code class loads.
     *
     * The intention is that the programmer will override this method
     * in their custom code to implement custom initialization logic.
     */
    open fun onLoad() {
        // Override in subclass
    }
}
