package org.missionpinball.mpf.devices

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.missionpinball.mpf.core.MachineController
import org.missionpinball.mpf.core.SystemWideDevice
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Score queues for SS games.
 *
 * Add scores over time and play a lot of chimes. Used in solid state (SS) games
 * to queue up scoring events and play them out sequentially with chime sounds,
 * rather than adding all the score at once.
 */
class ScoreQueue(machine: MachineController, name: String) : SystemWideDevice(machine, name) {

    override val configSection = "score_queues"
    override val collection = "score_queues"
    override val classLabel = "score_queue"

    /**
     * Queue of score values to add.
     */
    private val scoreQueue = Channel<Int>(Channel.UNLIMITED)

    /**
     * Deferred that completes when score queue is empty.
     */
    private val scoreQueueEmpty = CompletableDeferred<Unit>()

    /**
     * Background job handling the score queue.
     */
    private var scoreTask: Job? = null

    init {
        scoreQueueEmpty.complete(Unit)

        // TODO: Register async handler when event system supports it
        /*
        machine.events.addAsyncHandler("ball_ending", ::blockBallEndIfScoring)
        */
    }

    /**
     * Block ball ending until scoring is done.
     */
    private suspend fun blockBallEndIfScoring() {
        scoreQueueEmpty.await()
    }

    override suspend fun initialize() {
        super.initialize()

        // Start background task to handle score queue
        scoreTask = machine.scope.launch {
            handleScoreQueue()
        }
    }

    /**
     * Score a value via the queue.
     *
     * @param value Score value to add
     */
    fun score(value: Int) {
        val game = machine.game
        val player = game?.player

        if (game == null || player == null) {
            warningLog("Trying to use score_queue without an active game or player")
            return
        }

        scoreQueue.trySend(value)

        // Mark queue as not empty
        if (scoreQueueEmpty.isCompleted) {
            // Create a new deferred for the next round
            // Note: In a real implementation we'd need better synchronization here
        }
    }

    /**
     * Stop the score queue device.
     */
    fun stopDevice() {
        scoreTask?.cancel()
        scoreTask = null
    }

    /**
     * Handle score queue background processing.
     *
     * This processes scores one at a time, breaking them down into individual
     * digit scores and playing corresponding chimes.
     */
    private suspend fun handleScoreQueue() {
        while (isActive) {
            var score = scoreQueue.receive()
            debugLog("Scoring $score")

            while (score > 0) {
                // Get the position of the highest digit
                val digitPos = floor(log10(score.toDouble())).toInt()
                val digitScore = 10.0.pow(digitPos).toInt()

                // Score this amount
                val game = machine.game
                val player = game?.player
                if (player != null) {
                    player[name] = (player[name] as? Int ?: 0) + digitScore
                }

                // Reduce the remaining amount
                score -= digitScore

                debugLog("Scoring $digitScore on digit $digitPos. Remaining: $score")

                // Wait if there is a chime for that digit
                val chimes = config["chimes"] as? List<*> ?: emptyList<Any>()
                if (chimes.size > digitPos) {
                    val chimeIndex = chimes.size - (digitPos + 1)
                    val chime = chimes[chimeIndex] as? Driver

                    if (chime != null) {
                        chime.pulse()

                        val delaySeconds = (config["delay"] as? Number)?.toDouble() ?: 0.3
                        debugLog("Played chime for pos $digitPos. Waiting ${delaySeconds}s")

                        delay((delaySeconds * 1000).toLong())
                    }
                }
            }

            // Check if queue is empty
            if (scoreQueue.isEmpty) {
                // TODO: Complete scoreQueueEmpty deferred
                // In production code, we'd need better synchronization
            }
        }
    }
}
