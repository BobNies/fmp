package org.missionpinball.mpf.core

import java.util.UUID
import kotlin.random.Random

/**
 * Generic list randomizer.
 *
 * Provides weighted random selection from a list of items with various modes:
 * - Force different: Don't repeat the last selected item
 * - Force all: Ensure all items are selected before repeating
 * - Disable random: Sequential selection
 * - Looping: Whether to restart when all items exhausted
 *
 * @param T Type of items to randomize
 */
class Randomizer<T>(
    items: Any,
    private val machine: MachineController? = null,
    private val templateType: String? = null
) : Iterator<T> {

    /**
     * Fallback value to return when no valid items available.
     */
    var fallbackValue: T? = null

    /**
     * Force selecting a different item than the last one.
     */
    var forceDifferent = true

    /**
     * Force selecting all items before allowing repeats.
     */
    var forceAll = false

    /**
     * Disable randomization (sequential selection).
     */
    var disableRandom = false

    /**
     * List of items with their weights.
     */
    private val items = mutableListOf<Pair<T, Int>>()

    /**
     * Whether to loop when all items exhausted.
     */
    var loop: Boolean = true
        set(value) {
            field = value
            if (!value) {
                forceAll = true
            }
        }

    /**
     * Current state data.
     */
    private val data = mutableMapOf<String, Any?>()

    /**
     * Unique ID for this randomizer.
     */
    private val uuid = UUID.randomUUID()

    init {
        when (items) {
            is List<*> -> {
                for (item in items) {
                    when (item) {
                        is Pair<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            val thisItem = processTemplateIfNeeded(item.first as T)
                            val thisWeight = (item.second as? Number)?.toInt() ?: 1
                            this.items.add(Pair(thisItem, thisWeight))
                        }
                        is List<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            val thisItem = processTemplateIfNeeded(item[0] as T)
                            val thisWeight = (item.getOrNull(1) as? Number)?.toInt() ?: 1
                            this.items.add(Pair(thisItem, thisWeight))
                        }
                        else -> {
                            @Suppress("UNCHECKED_CAST")
                            val thisItem = processTemplateIfNeeded(item as T)
                            this.items.add(Pair(thisItem, 1))
                        }
                    }
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val itemMap = items as Map<T, Any>
                for ((thisItem, weight) in itemMap) {
                    val processedItem = processTemplateIfNeeded(thisItem)
                    val thisWeight = (weight as? Number)?.toInt() ?: 1
                    this.items.add(Pair(processedItem, thisWeight))
                }
                // Sort by item name if items have names
                this.items.sortBy { (item, _) ->
                    when (item) {
                        is Any -> item.toString()
                        else -> ""
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Invalid input for Randomizer")
            }
        }

        initData()
    }

    /**
     * Process item through template system if needed.
     */
    private fun processTemplateIfNeeded(item: T): T {
        // TODO: Implement template processing when placeholder manager is available
        if (machine != null && templateType != null) {
            // return generateTemplate(machine, templateType, item)
        }
        return item
    }

    /**
     * Initialize state data.
     */
    private fun initData() {
        data["current_item"] = null
        data["items_sent"] = mutableSetOf<T>()
        data["current_item_index"] = 0
    }

    override fun hasNext(): Boolean {
        // In loop mode, always has next
        if (loop) return true

        // In force_all mode, check if there are unsent items
        return if (forceAll) {
            @Suppress("UNCHECKED_CAST")
            val itemsSent = data["items_sent"] as Set<T>
            itemsSent.size < items.size
        } else {
            true
        }
    }

    override fun next(): T {
        return getNext()
    }

    /**
     * Get the next item.
     *
     * @param conditionalArgs Arguments for conditional evaluation (not yet implemented)
     * @return Next item
     */
    fun getNext(conditionalArgs: Map<String, Any?> = emptyMap()): T {
        if (disableRandom) {
            return nextNotRandom(conditionalArgs)
        }

        val availableItems = getItems(conditionalArgs)
        var potentialNexts = listOf<Pair<T, Int>>()

        @Suppress("UNCHECKED_CAST")
        val itemsSent = data["items_sent"] as MutableSet<T>
        val currentItem = data["current_item"] as? T

        if (forceAll) {
            potentialNexts = availableItems.filter { (item, _) -> item !in itemsSent }
        } else if (forceDifferent) {
            potentialNexts = availableItems.filter { (item, _) -> item != currentItem }
        }

        if (potentialNexts.isEmpty()) {
            if (!loop) {
                throw NoSuchElementException()
            }

            itemsSent.clear()

            // force different only works with more than 1 element
            potentialNexts = if (forceDifferent && items.size > 1) {
                availableItems.filter { (item, _) -> item != currentItem }
            } else {
                availableItems
            }
        }

        // If no values were found due to all conditions failing, return the fallback
        if (potentialNexts.isEmpty()) {
            return fallbackValue ?: throw NoSuchElementException("No items available")
        }

        val selected = pickWeightedRandom(potentialNexts)
        data["current_item"] = selected
        itemsSent.add(selected)

        return selected
    }

    /**
     * Get next item in non-random (sequential) mode.
     */
    private fun nextNotRandom(conditionalArgs: Map<String, Any?>): T {
        var currentIndex = data["current_item_index"] as Int

        if (currentIndex >= items.size) {
            if (!loop) {
                throw NoSuchElementException()
            }
            currentIndex = 0
        }

        val availableItems = getItems(conditionalArgs)
        val selected = availableItems[currentIndex].first

        data["current_item"] = selected
        data["current_item_index"] = currentIndex + 1

        return selected
    }

    /**
     * Get current item (or get next if none set).
     *
     * @return Current item
     */
    fun getCurrent(): T {
        val current = data["current_item"] as? T
        return current ?: getNext()
    }

    /**
     * Get items (with conditional filtering if applicable).
     */
    private fun getItems(conditionalArgs: Map<String, Any?>): List<Pair<T, Int>> {
        // TODO: Implement conditional filtering when placeholder manager is available
        /*
        if (templateType != null) {
            val conditionalItems = mutableListOf<Pair<T, Int>>()
            for ((item, weight) in items) {
                // Check if item passes conditional evaluation
                if (evaluateCondition(item, conditionalArgs)) {
                    conditionalItems.add(Pair(item, weight))
                }
            }
            return conditionalItems
        }
        */
        return items
    }

    companion object {
        /**
         * Pick a weighted random item from a list.
         *
         * @param items List of items with weights
         * @return Selected item
         */
        fun <T> pickWeightedRandom(items: List<Pair<T, Int>>): T {
            val totalWeights = items.sumOf { it.second }
            val value = Random.nextInt(1, totalWeights + 1)
            var indexValue = 0

            for ((item, weight) in items) {
                indexValue += weight
                if (indexValue >= value) {
                    return item
                }
            }

            return items.last().first
        }

        /**
         * Generate a template from a value.
         *
         * TODO: Implement when placeholder manager is available
         */
        fun generateTemplate(machine: MachineController, templateType: String, value: Any): Any {
            // if (templateType == "event") {
            //     return machine.placeholderManager.parseConditionalTemplate(value)
            // }
            return value
        }
    }
}
