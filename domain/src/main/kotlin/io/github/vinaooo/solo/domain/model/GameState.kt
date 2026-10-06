package io.github.vinaooo.solo.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of a Klondike game. In every pile the last card is the top card.
 */
@Serializable
data class GameState(
    val stock: List<Card>,
    val waste: List<Card>,
    val foundations: List<List<Card>>,
    val tableau: List<List<Card>>,
    val drawMode: DrawMode,
    val score: Int = 0,
    val moves: Int = 0,
    val recycles: Int = 0,
    val elapsedSeconds: Long = 0,
    val mode: GameMode = GameMode.STANDARD,
    /** Games saved before difficulty existed were any shuffle: Hard. */
    val difficulty: Difficulty = Difficulty.HARD,
) {
    val isWon: Boolean
        get() = foundations.all { it.size == Rank.entries.size }

    /** The time left to win in, or null when the mode has no limit. */
    val secondsLeft: Long?
        get() = mode.timeLimitSeconds(drawMode)?.let { (it - elapsedSeconds).coerceAtLeast(0) }

    /** The time limit has run out before the game was won: no more moves. */
    val isTimeUp: Boolean
        get() = !isWon && secondsLeft == 0L

    /** Whether the waste may still go back to the stock. */
    val canRecycle: Boolean
        get() = mode.recycleLimit(drawMode)?.let { recycles < it } ?: true

    fun allCards(): List<Card> = stock + waste + foundations.flatten() + tableau.flatten()

    companion object {
        const val TABLEAU_COUNT = 7
        const val FOUNDATION_COUNT = 4
    }
}
