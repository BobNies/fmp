package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice

/**
 * Routes balls from one device to another when captured.
 *
 * This mode device claims balls when they enter source devices and
 * automatically ejects them to a target device. It tracks balls in
 * flight and ensures proper claiming at the target.
 *
 * Note: In Python, this uses @DeviceMonitor("balls_routing") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * Note: In Python, this extends EnableDisableMixin.
 * In Kotlin, we implement enable/disable functionality directly.
 */
class BallRouting(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "ball_routings"
    override val collection = "ball_routings"
    override val classLabel = "ball_routing"

    /**
     * Queue tracking how many balls from each device need routing.
     */
    private val routingQueue = mutableMapOf<BallDevice, Int>().withDefault { 0 }

    /**
     * Number of balls currently being routed to the target.
     */
    private var ballsAtTarget = 0

    /**
     * Event handler keys for cleanup.
     */
    private val handlers = mutableListOf<Any>()

    /**
     * Whether the routing is enabled.
     */
    private var _enabled = false

    /**
     * Enable ball routing.
     */
    private fun enable() {
        _enabled = true

        val sourceDevices = config["source_devices"] as? List<*> ?: emptyList<BallDevice>()
        for (device in sourceDevices) {
            val ballDevice = device as? BallDevice ?: continue

            // Register handler for ball_enter (claim phase)
            // TODO: Register event handler when available
            /*
            val handler1 = machine.events.addHandler(
                "balldevice_${ballDevice.name}_ball_enter",
                { kwargs -> claimBalls(ballDevice, kwargs) },
                priority = mode?.priority ?: 0
            )
            handlers.add(handler1)

            // Register handler for ball_entered (route phase)
            val handler2 = machine.events.addHandler(
                "balldevice_${ballDevice.name}_ball_entered",
                { routeBall(ballDevice) },
                priority = mode?.priority ?: 0
            )
            handlers.add(handler2)
            */
        }

        // Register handler for target device ball_enter
        val targetDevice = config["target_device"] as? BallDevice
        if (targetDevice != null) {
            // TODO: Register event handler when available
            /*
            val handler3 = machine.events.addHandler(
                "balldevice_${targetDevice.name}_ball_enter",
                ::addBall,
                priority = (mode?.priority ?: 0) + 10000
            )
            handlers.add(handler3)
            */
        }

        debugLog("Enabling")
    }

    /**
     * Claim balls to route them to destination later.
     *
     * @param device Device where balls were captured
     * @param unclaimedBalls Number of unclaimed balls
     * @return Map with updated unclaimed_balls count (0 to claim all)
     */
    private fun claimBalls(device: BallDevice, unclaimedBalls: Int): Map<String, Any> {
        if (!_enabled) {
            return emptyMap()
        }

        // Remember how many balls were captured
        routingQueue[device] = (routingQueue[device] ?: 0) + unclaimedBalls

        debugLog("Claiming $unclaimedBalls balls from ${device.name}")

        // Claim all balls
        return mapOf("unclaimed_balls" to 0)
    }

    /**
     * Route balls to destination.
     *
     * @param device Device to eject from
     */
    private fun routeBall(device: BallDevice) {
        val ballsToRoute = routingQueue[device] ?: 0
        if (ballsToRoute == 0) return

        ballsAtTarget += ballsToRoute

        val targetDevice = config["target_device"] as? BallDevice

        debugLog("Routing $ballsToRoute balls from ${device.name} to ${targetDevice?.name}")

        // Eject all queued balls to target
        for (i in 0 until ballsToRoute) {
            // TODO: Eject when ball device is available
            /*
            device.eject(target = targetDevice)
            */
        }

        routingQueue[device] = 0
    }

    /**
     * Mark balls as unclaimed at destination.
     *
     * @param unclaimedBalls Number of balls not yet claimed
     * @param newBalls Total new balls entering
     * @return Map with updated unclaimed_balls count
     */
    private fun addBall(unclaimedBalls: Int, newBalls: Int): Map<String, Any> {
        val claimedBalls = newBalls - unclaimedBalls

        if (ballsAtTarget > 0 && claimedBalls > 0) {
            val targetDevice = config["target_device"] as? BallDevice

            if (claimedBalls <= ballsAtTarget) {
                val newUnexpected = unclaimedBalls + ballsAtTarget
                debugLog("Adding $newUnexpected balls to target ${targetDevice?.name}")
                ballsAtTarget = 0
                return mapOf("unclaimed_balls" to newUnexpected)
            }

            debugLog("Adding $claimedBalls balls to target ${targetDevice?.name}")
            ballsAtTarget -= claimedBalls
            return mapOf("unclaimed_balls" to newBalls)
        }

        return emptyMap()
    }

    /**
     * Disable ball routing.
     */
    private fun disable() {
        debugLog("Disabling")
        _enabled = false

        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandlersByKeys(handlers)
        */
        handlers.clear()
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)
        debugLog("Removing")
        disable()
    }

    /**
     * Number of balls currently being routed.
     */
    val ballsRouting: Int
        get() = ballsAtTarget
}

/**
 * Ball device interface.
 *
 * TODO: Implement full ball device when available
 */
interface BallDevice {
    val name: String

    fun eject(target: BallDevice?)
}
