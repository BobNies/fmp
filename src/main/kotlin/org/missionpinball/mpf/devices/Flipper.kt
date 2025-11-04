package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice

/**
 * Represents a flipper in a pinball machine.
 *
 * Contains several methods for actions that can be performed on this flipper,
 * like enable(), disable(), etc.
 *
 * Flippers have several options, including player buttons, EOS switches,
 * multiple coil options (pulsing, hold coils, etc.)
 *
 * Note: In Python, this uses @DeviceMonitor("_enabled") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 */
class Flipper(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "flippers"
    override val collection = "flippers"
    override val classLabel = "flipper"

    /**
     * Whether the flipper is enabled (hardware rules active).
     */
    private var _enabled = false

    /**
     * List of active hardware rules.
     */
    private val activeRules = mutableListOf<Any>()

    /**
     * Whether the flipper was flipped via software (sw_flip).
     */
    private var swFlipped = false

    override suspend fun initialize() {
        super.initialize()

        if (config["include_in_ball_search"] as? Boolean == true) {
            // TODO: Register ball search when available
            /*
            val playfield = config["playfield"]
            val ballSearchOrder = config["ball_search_order"] as? Int
            playfield?.ballSearch?.register(ballSearchOrder, ::ballSearch, name)
            */
        }
    }

    /**
     * Event handler for enable event.
     *
     * To prevent multiple rules at the same time we prioritize disable > enable.
     *
     * TODO: Add @EventHandler(1) annotation when event system is fully integrated
     */
    fun eventEnable() {
        enable()
    }

    /**
     * Enable the flipper by writing the necessary hardware rules to the hardware controller.
     *
     * The hardware rules for coils can be kind of complex given all the
     * options, so we've mapped all the options out here. We literally have
     * methods to enable the various rules based on the rule letters here,
     * which we've implemented below. Keeps it easy to understand. :)
     *
     * Note there's a platform feature saved at:
     * machine.config['platform']['hw_enable_auto_disable']. If True, it
     * means that the platform hardware rules will automatically disable a coil
     * that has been enabled when the trigger switch is disabled. If False, it
     * means the hardware platform needs its own rule to disable the coil when
     * the switch is disabled.
     *
     * Two coils, using EOS switch to indicate the end of the power stroke:
     * Rule  Type     Coil  Switch  Action
     * A.    Enable   Main  Button  active
     * D.    Enable   Hold  Button  active
     * E.    Disable  Main  EOS     active
     *
     * One coil, using EOS switch:
     * Rule  Type     Coil  Switch  Action
     * A.    Enable   Main  Button  active
     * H.    PWM      Main  EOS     active
     *
     * Two coils, not using EOS switch:
     * Rule  Type     Coil  Switch  Action
     * B.    Pulse    Main  Button  active
     * D.    Enable   Hold  Button  active
     *
     * One coil, not using EOS switch:
     * Rule  Type       Coil  Switch  Action
     * C.    Pulse/PWM  Main  button  active
     *
     * Use EOS switch for safety (for platforms that support multiple switch
     * rules). Note that this rule is the letter "i", not a numeral 1.
     * I. Enable power if button is active and EOS is not active
     */
    fun enable() {
        // Prevent duplicate enable
        if (_enabled) {
            return
        }

        _enabled = true

        debugLog("Enabling flipper with config: $config")

        // Apply the proper hardware rules for our config
        if (config["activation_switch"] != null) {
            // Only add rules if we are using a switch
            when {
                config["use_eos"] == true -> {
                    enableMainCoilEosCutoffRule()
                }
                config["hold_coil"] != null -> {
                    enableMainCoilPulseRule()
                }
                else -> {
                    enableSingleCoilRule()
                }
            }

            if (config["hold_coil"] != null) {
                enableHoldCoilRule()
            }
        }
    }

    /**
     * Event handler for disable event.
     *
     * To prevent multiple rules at the same time we prioritize disable > enable.
     *
     * TODO: Add @EventHandler(10) annotation when event system is fully integrated
     */
    fun eventDisable() {
        debugLog("Disabling via event callback")
        disable()
    }

    /**
     * Disable the flipper.
     *
     * This method makes it so the cabinet flipper buttons no longer control
     * the flippers. Used when no game is active and when the player has
     * tilted.
     */
    fun disable() {
        if (!_enabled) {
            return
        }

        debugLog("Disabling")

        for (rule in activeRules) {
            // Disable all rules
            // TODO: Clear hardware rule when platform controller is available
            /*
            machine.platformController.clearHwRule(rule)
            */
        }

        if (swFlipped) {
            // Disable the coils if activated via sw_flip
            swRelease()
        }

        activeRules.clear()
        _enabled = false
    }

    /**
     * Return pulse_ms setting.
     */
    private fun getPulseMs(): Int? {
        val mainCoilOverwrite = config["main_coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        var pulseMs = mainCoilOverwrite["pulse_ms"] as? Int

        val powerSettingName = config["power_setting_name"] as? String
        if (powerSettingName != null) {
            // TODO: Get settings factor when settings controller is available
            /*
            val settingsFactor = machine.settings.getSettingValue(powerSettingName) as? Double ?: 1.0
            if (pulseMs == null) {
                pulseMs = machine.config["mpf"]?.get("default_pulse_ms") as? Int ?: 10
            }
            return (pulseMs * settingsFactor).toInt()
            */
        }

        return pulseMs
    }

    /**
     * Return pulse_ms for hold coil.
     */
    private fun getHoldPulseMs(): Int? {
        val holdCoilOverwrite = config["hold_coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        var pulseMs = holdCoilOverwrite["pulse_ms"] as? Int

        val powerSettingName = config["power_setting_name"] as? String
        if (powerSettingName != null) {
            // TODO: Get settings factor when settings controller is available
            /*
            val settingsFactor = machine.settings.getSettingValue(powerSettingName) as? Double ?: 1.0
            if (pulseMs == null) {
                pulseMs = machine.config["mpf"]?.get("default_pulse_ms") as? Int ?: 10
            }
            return (pulseMs * settingsFactor).toInt()
            */
        }

        return pulseMs
    }

    /**
     * Return pulse_power.
     */
    private fun getPulsePower(): Double? {
        val mainCoilOverwrite = config["main_coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        return mainCoilOverwrite["pulse_power"] as? Double
    }

    /**
     * Return pulse_power for hold coil.
     */
    private fun getHoldPulsePower(): Double? {
        val holdCoilOverwrite = config["hold_coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        return holdCoilOverwrite["pulse_power"] as? Double
    }

    /**
     * Return hold_power.
     */
    private fun getHoldPower(): Double? {
        val mainCoilOverwrite = config["main_coil_overwrite"] as? Map<*, *> ?: emptyMap<String, Any>()
        return mainCoilOverwrite["hold_power"] as? Double
    }

    /**
     * Enable single coil rule (Rule C).
     */
    private fun enableSingleCoilRule() {
        debugLog("Enabling single coil rule")

        // TODO: Set hardware rule when platform controller is available
        /*
        val rule = machine.platformController.setPulseOnHitAndEnableAndReleaseRule(
            SwitchRuleSettings(switch = config["activation_switch"], debounce = false, invert = false),
            DriverRuleSettings(driver = config["main_coil"], recycle = false),
            PulseRuleSettings(duration = getPulseMs(), power = getPulsePower()),
            HoldRuleSettings(power = getHoldPower())
        )
        activeRules.add(rule)
        */
    }

    /**
     * Enable main coil pulse rule (Rule B).
     */
    private fun enableMainCoilPulseRule() {
        debugLog("Enabling main coil pulse rule")

        // TODO: Set hardware rule when platform controller is available
        /*
        val rule = machine.platformController.setPulseOnHitAndReleaseRule(
            SwitchRuleSettings(switch = config["activation_switch"], debounce = false, invert = false),
            DriverRuleSettings(driver = config["main_coil"], recycle = false),
            PulseRuleSettings(duration = getPulseMs(), power = getPulsePower())
        )
        activeRules.add(rule)
        */
    }

    /**
     * Enable hold coil rule (Rule D).
     */
    private fun enableHoldCoilRule() {
        debugLog("Enabling hold coil rule")

        // TODO: Set hardware rule when platform controller is available
        /*
        val rule = machine.platformController.setPulseOnHitAndEnableAndReleaseRule(
            SwitchRuleSettings(switch = config["activation_switch"], debounce = false, invert = false),
            DriverRuleSettings(driver = config["hold_coil"], recycle = false),
            PulseRuleSettings(duration = getHoldPulseMs(), power = getHoldPulsePower()),
            HoldRuleSettings(power = getHoldPower())
        )
        activeRules.add(rule)
        */
    }

    /**
     * Enable main coil EOS cutoff rule (Rules A, E, or H).
     */
    private fun enableMainCoilEosCutoffRule() {
        if (config["hold_coil"] != null) {
            debugLog("Enabling main coil EOS cutoff rule w/o hold")

            // TODO: Set hardware rule when platform controller is available
            /*
            val rule = machine.platformController.setPulseOnHitAndReleaseAndDisableRule(
                SwitchRuleSettings(switch = config["activation_switch"], debounce = false, invert = false),
                SwitchRuleSettings(switch = config["eos_switch"], debounce = false, invert = false),
                DriverRuleSettings(driver = config["main_coil"], recycle = false),
                PulseRuleSettings(duration = getHoldPulseMs(), power = getHoldPulsePower()),
                EosRuleSettings(
                    enableRepulse = config["repulse_on_eos_open"] as? Boolean ?: false,
                    debounceMs = config["eos_active_ms_before_repulse"] as? Int ?: 0
                )
            )
            activeRules.add(rule)
            */
        } else {
            debugLog("Enabling main coil EOS cutoff rule w/ hold")

            // TODO: Set hardware rule when platform controller is available
            /*
            val rule = machine.platformController.setPulseOnHitAndEnableAndReleaseAndDisableRule(
                SwitchRuleSettings(switch = config["activation_switch"], debounce = false, invert = false),
                SwitchRuleSettings(switch = config["eos_switch"], debounce = false, invert = false),
                DriverRuleSettings(driver = config["main_coil"], recycle = false),
                PulseRuleSettings(duration = getHoldPulseMs(), power = getHoldPulsePower()),
                HoldRuleSettings(power = getHoldPower()),
                EosRuleSettings(
                    enableRepulse = config["repulse_on_eos_open"] as? Boolean ?: false,
                    debounceMs = config["eos_active_ms_before_repulse"] as? Int ?: 0
                )
            )
            activeRules.add(rule)
            */
        }
    }

    /**
     * Event handler for sw_flip event.
     *
     * TODO: Add @EventHandler(6) annotation when event system is fully integrated
     */
    fun eventSwFlip() {
        swFlip()
    }

    /**
     * Activate the flipper via software as if the flipper button was pushed.
     *
     * This is needed because the real flipper activations are handled in
     * hardware, so if you want to flip the flippers with the keyboard or OSC
     * interfaces, you have to call this method.
     *
     * Note this method will keep this flipper enabled until you call
     * swRelease().
     */
    fun swFlip() {
        debugLog("sw_flip")

        if (!_enabled) {
            return
        }

        swFlipped = true

        val mainCoil = config["main_coil"] as? Driver
        val holdCoil = config["hold_coil"] as? Driver

        if (holdCoil != null) {
            mainCoil?.pulse()
            holdCoil.enable()
        } else {
            mainCoil?.enable()
        }
    }

    /**
     * Event handler for sw_release event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventSwRelease() {
        swRelease()
    }

    /**
     * Deactivate the flipper via software as if the flipper button was released.
     *
     * See the documentation for swFlip() for details.
     */
    fun swRelease() {
        debugLog("sw_release")
        swFlipped = false

        // Disable the flipper coil(s)
        val mainCoil = config["main_coil"] as? Driver
        val holdCoil = config["hold_coil"] as? Driver

        mainCoil?.disable()
        holdCoil?.disable()
    }

    /**
     * Ball search callback.
     *
     * @param phase Ball search phase
     * @param iteration Ball search iteration
     * @return True if this device can help with ball search
     */
    private fun ballSearch(phase: Int, iteration: Int): Boolean {
        swFlip()

        val ballSearchHoldTime = (config["ball_search_hold_time"] as? Number)?.toLong() ?: 1000L

        // TODO: Use machine delay when available
        /*
        machine.delay.add(ballSearchHoldTime, ::swRelease, "flipper_${name}_ball_search")
        */

        return true
    }
}
