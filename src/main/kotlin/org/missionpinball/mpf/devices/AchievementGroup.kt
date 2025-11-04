package org.missionpinball.mpf.devices

import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.Mode
import org.missionpinball.mpf.core.ModeDevice
import org.missionpinball.mpf.core.Player
import kotlin.random.Random

/**
 * An achievement group in a pinball machine.
 *
 * Achievement groups manage collections of achievements, handling selection,
 * rotation, and tracking when all achievements are complete. They can
 * automatically select achievements, rotate through available achievements,
 * and enable/disable based on achievement states.
 *
 * Note: In Python, this uses @DeviceMonitor("_enabled", "_selected_member") decorator.
 * In Kotlin, monitoring will be implemented when device monitoring is available.
 *
 * TODO: Add @EventHandler annotations when event system is fully integrated
 */
class AchievementGroup(machine: MachineController, name: String) : ModeDevice(machine, name) {

    override val configSection = "achievement_groups"
    override val collection = "achievement_groups"
    override val classLabel = "achievement_group"

    /**
     * Currently playing show.
     */
    private var _show: Any? = null

    /**
     * Whether the group is enabled.
     */
    private var _enabled = false

    /**
     * Whether the group has been loaded in a mode.
     */
    private var _loaded = false

    /**
     * Currently selected achievement member.
     */
    private var _selectedMember: Achievement? = null

    /**
     * Whether rotation is in progress.
     */
    private var _rotationInProgress = false

    /**
     * Event handler keys for cleanup.
     */
    private val handlers = mutableListOf<Any>()

    /**
     * Whether the group is enabled.
     */
    val enabled: Boolean
        get() = _enabled

    /**
     * Currently selected achievement.
     */
    val selectedMember: Achievement?
        get() = _selectedMember

    override fun enable() {
        if (!_loaded) {
            return
        }

        if (_enabled) {
            debugLog("Group is already enabled. Aborting")
            return
        }

        val disableWhileStarted = config["disable_while_achievement_started"] as? Boolean ?: false
        if (isMemberStarted() && disableWhileStarted) {
            debugLog("Not enabling because a member is started and disable_while_achievement_started is true.")
            return
        }

        super.enable()
        debugLog("Call to enable this group")

        _enabled = true
        debugLog("Enabling group")

        stopShow()

        if (_selectedMember?.selected == true) {
            _selectedMember = null
        }

        val show = config["show_when_enabled"]
        if (show != null) {
            // TODO: Play show when show controller is available
            /*
            val showTokens = config["show_tokens"] as? Map<String, Any>
            val syncMs = config["sync_ms"] as? Long

            _show = show.play(
                priority = mode?.priority ?: 0,
                loops = -1,
                syncMs = syncMs,
                showTokens = showTokens
            )
            */
        }

        val eventsWhenEnabled = config["events_when_enabled"] as? List<*> ?: emptyList<String>()
        for (event in eventsWhenEnabled) {
            machine.events.post(event.toString())
        }

        processCurrentMemberState()
    }

    /**
     * Event handler for disable event.
     *
     * TODO: Add @EventHandler(0) annotation when event system is fully integrated
     */
    fun eventDisable() {
        disable()
    }

    override fun disable() {
        if (!_enabled) {
            debugLog("Group is already disabled. Aborting")
            return
        }
        debugLog("Disabling group")
        stopShow()
        _enabled = false
    }

    /**
     * Stop the currently playing show.
     */
    private fun stopShow() {
        _show?.let { show ->
            debugLog("Stopping show")
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
            _show = null
        }
    }

    /**
     * Get achievements that can be selected and started.
     */
    private fun getAvailableAchievementsForSelection(): List<Achievement> {
        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()
        return achievements.mapNotNull { it as? Achievement }
            .filter { it.canBeSelectedForStart }
    }

    /**
     * Get the current selected achievement.
     */
    private fun getCurrent(): Achievement? {
        debugLog("Getting current selected achievement")
        if (_selectedMember == null) {
            selectRandomAchievement()
        }
        return _selectedMember
    }

    /**
     * Event handler for start_selected event.
     *
     * TODO: Add @EventHandler(5) annotation when event system is fully integrated
     */
    fun eventStartSelected() {
        startSelected()
    }

    /**
     * Start the currently selected achievement.
     */
    fun startSelected() {
        debugLog("Call to start selected achievement")
        if (!_enabled) {
            debugLog("Group not enabled. Aborting")
            return
        }

        try {
            getCurrent()?.start()
        } catch (e: Exception) {
            // Don't have a current one
        }
    }

    /**
     * Event handler for rotate_right event.
     *
     * TODO: Add @EventHandler(6) annotation when event system is fully integrated
     */
    fun eventRotateRight(reverse: Boolean = false) {
        rotateRight(reverse)
    }

    /**
     * Rotate to the right.
     *
     * @param reverse If true, rotate left instead
     */
    fun rotateRight(reverse: Boolean = false) {
        debugLog("Call to rotate")
        if (!isOkToChangeSelection()) {
            return
        }

        val autoSelect = config["auto_select"] as? Boolean ?: true
        if (_selectedMember == null && !autoSelect) {
            debugLog("Nothing selected and auto_select false. Abort.")
            return
        }

        _rotationInProgress = true

        // If there's already one selected, set it back to enabled
        if (_selectedMember?.selected == true) {
            _selectedMember?.unselect()
        }

        val achievements = getAvailableAchievementsForSelection()
        if (achievements.isEmpty()) {
            // There is nothing to rotate
            debugLog("Nothing to rotate. Abort.")
            return
        }

        try {
            val current = getCurrent()
            val currentIndex = achievements.indexOf(current)

            _selectedMember = if (currentIndex >= 0) {
                if (reverse) {
                    achievements[(currentIndex - 1 + achievements.size) % achievements.size]
                } else {
                    achievements[(currentIndex + 1) % achievements.size]
                }
            } else {
                getCurrent()
            }
        } catch (e: Exception) {
            _selectedMember = getCurrent()
        }

        _selectedMember?.select()
        _rotationInProgress = false
    }

    /**
     * Event handler for rotate_left event.
     *
     * TODO: Add @EventHandler(7) annotation when event system is fully integrated
     */
    fun eventRotateLeft() {
        rotateLeft()
    }

    /**
     * Rotate to the left.
     */
    fun rotateLeft() {
        rotateRight(reverse = true)
    }

    /**
     * Post event when no more enabled achievements are available.
     */
    private fun noMoreEnabled() {
        debugLog("No more achievements are enabled")
        val events = config["events_when_no_more_enabled"] as? List<*> ?: emptyList<String>()
        for (event in events) {
            machine.events.post(event.toString())
        }
    }

    /**
     * Post event when all achievements are complete.
     */
    private fun allComplete() {
        debugLog("All achievements are complete")
        disable()  // Disable before event post so event can re-enable

        val events = config["events_when_all_completed"] as? List<*> ?: emptyList<String>()
        for (event in events) {
            machine.events.post(event.toString())
        }
    }

    /**
     * Event handler for select_random_achievement event.
     *
     * TODO: Add @EventHandler(9) annotation when event system is fully integrated
     */
    fun eventSelectRandomAchievement() {
        val disableRandom = config["disable_random"] as? Boolean ?: false
        if (disableRandom) {
            rotateRight()
        } else {
            selectRandomAchievement()
        }
    }

    /**
     * Select a random or sequential achievement.
     */
    fun selectRandomAchievement() {
        debugLog("Selecting an achievement")

        if (!isOkToChangeSelection()) {
            debugLog("Not ok to change selection. Aborting")
            return
        }

        if (_selectedMember?.selected == true) {
            _selectedMember?.unselect()
        }

        try {
            val achievements = getAvailableAchievementsForSelection()
            val disableRandom = config["disable_random"] as? Boolean ?: false

            val ach = if (disableRandom) {
                achievements.first()
            } else {
                achievements.random()
            }

            debugLog("Picked new ${if (disableRandom) "non-random" else "random"} achievement: $ach")
            _selectedMember = ach
            ach.select()
        } catch (e: Exception) {
            noMoreEnabled()
        }
    }

    /**
     * Check if it's ok to change selection.
     */
    private fun isOkToChangeSelection(): Boolean {
        debugLog("Checking if it's ok to change selection...")
        if (_enabled) {
            debugLog("Ok because enabled")
            return true
        }

        val allowChangeWhileDisabled = config["allow_selection_change_while_disabled"] as? Boolean ?: false
        if (allowChangeWhileDisabled) {
            debugLog("Ok because allow_selection_change_while_disabled is set (but disabled)")
            return true
        }

        debugLog("not ok")
        return false
    }

    /**
     * Notify the group that one of its members has changed state.
     */
    fun memberStateChanged() {
        if (!_loaded) {
            return
        }

        debugLog("Member state has changed")
        if (isMemberStarted()) {
            debugLog("A member is started")
            val disableWhileStarted = config["disable_while_achievement_started"] as? Boolean ?: false
            if (disableWhileStarted && enabled) {
                debugLog("disable_while_achievement_started is true")
                disable()
            } else {
                processCurrentMemberState()
            }
        } else {
            val enableWhileNoneStarted = config["enable_while_no_achievement_started"] as? Boolean ?: false
            if (enableWhileNoneStarted && !enabled) {
                debugLog("enable_while_no_achievement_started is true")
                enable()
            } else {
                processCurrentMemberState()
            }
        }
    }

    /**
     * Process the current member state.
     */
    private fun processCurrentMemberState() {
        debugLog("Processing current member state")

        if (_rotationInProgress) {
            debugLog("Rotation in progress. Aborting")
            return
        }

        if (!_enabled) {
            debugLog("Not enabled. Aborting")
            return
        }

        checkForAllComplete()
        if (checkForNoMoreEnabled()) {
            debugLog("No achievement enabled. Aborting.")
            return
        }
        updateSelected()

        val autoSelect = config["auto_select"] as? Boolean ?: true
        if (_selectedMember == null && autoSelect) {
            debugLog("No selected member, but auto_select is true")
            selectRandomAchievement()
        }
    }

    /**
     * Update the selected achievement.
     */
    private fun updateSelected(): Boolean {
        debugLog("Updating selected achievement")
        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()

        for (ach in achievements.mapNotNull { it as? Achievement }) {
            if (ach.selected) {
                _selectedMember = ach
                debugLog("Already have a selected member is $ach")
                return true
            }
        }

        debugLog("Do not have a current selected member")

        val autoSelect = config["auto_select"] as? Boolean ?: true
        if (autoSelect) {
            debugLog("Auto select is true. Getting achievement")
            selectRandomAchievement()
        }

        return false
    }

    /**
     * Check if all achievements are complete.
     */
    private fun checkForAllComplete(): Boolean {
        debugLog("Checking for all complete")
        if (!_enabled) {
            debugLog("Group is not enabled. Aborting...")
            return false
        }

        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()
        val incomplete = achievements.mapNotNull { it as? Achievement }
            .filter { it.state != "completed" }

        if (incomplete.isEmpty()) {
            allComplete()
            return true
        }

        debugLog("All are not complete")
        return false
    }

    /**
     * Check if no more achievements are enabled.
     */
    private fun checkForNoMoreEnabled(): Boolean {
        debugLog("Checking for no more enabled")

        if (getAvailableAchievementsForSelection().isEmpty()) {
            noMoreEnabled()
            return true
        }

        return false
    }

    /**
     * Check if any member is started.
     */
    private fun isMemberStarted(): Boolean {
        debugLog("Checking if member is started")
        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()

        for (ach in achievements.mapNotNull { it as? Achievement }) {
            if (ach.state == "started") {
                debugLog("Found $ach is started")
                return true
            }
        }

        debugLog("No member is started")
        return false
    }

    override fun deviceLoadedInMode(mode: Mode, player: Player) {
        super.deviceLoadedInMode(mode, player)
        _loaded = true

        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()
        for (ach in achievements.mapNotNull { it as? Achievement }) {
            // TODO: Register event handler when available
            /*
            val handler = machine.events.addHandler(
                "achievement_${ach.name}_changed_state",
                ::memberStateChanged
            )
            handlers.add(handler)
            */
        }

        val enableWhileNoneStarted = config["enable_while_no_achievement_started"] as? Boolean ?: false
        if (enableWhileNoneStarted && isMemberStarted() && !_enabled) {
            enable()
        }
    }

    override fun deviceRemovedFromMode(mode: Mode) {
        super.deviceRemovedFromMode(mode)

        // TODO: Remove event handlers when available
        /*
        machine.events.removeHandlersByKeys(handlers)
        */
        handlers.clear()

        disable()
        _loaded = false
        _selectedMember = null

        _show?.let { show ->
            // TODO: Stop show when show controller is available
            /*
            show.stop()
            */
        }
    }

    /**
     * Enable debugging on all related achievements.
     */
    private fun enableRelatedDeviceDebugging() {
        val achievements = config["achievements"] as? List<*> ?: emptyList<Achievement>()
        for (ach in achievements.mapNotNull { it as? Achievement }) {
            // TODO: Enable debugging when available
            /*
            ach.enableDebugging()
            */
        }
    }
}
