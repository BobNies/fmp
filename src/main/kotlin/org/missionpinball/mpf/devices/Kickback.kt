package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController

/**
 * A kickback device which will fire a ball back into the playfield.
 *
 * Kickbacks are a type of autofire coil that fires when a ball enters
 * a specific area, typically to prevent the ball from draining down an
 * outlane. They use hardware rules for instant response.
 *
 * Note: In Python, this uses @DeviceMonitor("_enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Kickback(machine: MachineController, name: String) : AutofireCoil(machine, name) {

    override val configSection = "kickbacks"
    override val collection = "kickbacks"
    override val classLabel = "kickback"

    /**
     * Override hit handler to post kickback-specific event.
     *
     * TODO: This requires hit() in AutofireCoil to be protected instead of private.
     */
    // Note: In Python, this overrides the _hit method from AutofireCoil
    // In Kotlin, we'll need to make sure hit() is protected in AutofireCoil
    fun onHit() {
        // Call parent logic (when AutofireCoil is updated to support this)
        // super.hit()

        // Post kickback-specific event
        machine.events.post("kickback_${name}_fired")
    }
}

/**
 * Event: kickback_(name)_fired
 *
 * Kickback fired a ball.
 */
