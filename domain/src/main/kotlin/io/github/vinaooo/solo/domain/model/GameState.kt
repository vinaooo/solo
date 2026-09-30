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
) {
    val isWon: Boolean
        get() = foundations.all { it.size == Rank.entries.size }

    fun allCards(): List<Card> = stock + waste + foundations.flatten() + tableau.flatten()

    companion object {
        const val TABLEAU_COUNT = 7
        const val FOUNDATION_COUNT = 4
    }
}
