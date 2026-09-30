package io.github.vinaooo.solo.domain.hint

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.KlondikeRules
import io.github.vinaooo.solo.domain.rules.RuleSet

/** Suggests useful moves, best first. Moves that cannot improve the position are left out. */
class HintEngine(private val rules: RuleSet = KlondikeRules()) {

    fun bestHint(state: GameState): Move? = rankedHints(state).firstOrNull()

    fun rankedHints(state: GameState): List<Move> = rules.legalMoves(state)
        .distinctBy { it.foundationSource() ?: it }
        .map { it to priority(state, it) }
        .filter { (_, priority) -> priority > USELESS }
        .sortedByDescending { (_, priority) -> priority }
        .map { (move, _) -> move }

    private fun priority(state: GameState, move: Move): Int = when (move) {
        is Move.TableauToFoundation -> if (revealsCard(state, move.from, 1)) REVEALING_FOUNDATION else FOUNDATION
        is Move.WasteToFoundation -> FOUNDATION
        is Move.TableauToTableau -> tableauPriority(state, move)
        is Move.WasteToTableau -> WASTE_TO_TABLEAU
        Move.Draw -> DRAW
        Move.Recycle -> RECYCLE
        is Move.FoundationToTableau -> USELESS
    }

    private fun tableauPriority(state: GameState, move: Move.TableauToTableau): Int {
        val emptiesColumn = move.count == state.tableau[move.from].size
        return when {
            revealsCard(state, move.from, move.count) -> REVEALS_CARD
            emptiesColumn && state.tableau[move.to].isNotEmpty() -> EMPTIES_COLUMN
            else -> USELESS
        }
    }

    private fun revealsCard(state: GameState, column: Int, count: Int): Boolean =
        state.tableau[column].dropLast(count).lastOrNull()?.isFaceUp == false

    /** Identifies moves of the same card to different foundations, so only one is suggested. */
    private fun Move.foundationSource(): Any? = when (this) {
        is Move.WasteToFoundation -> "waste"
        is Move.TableauToFoundation -> "tableau$from"
        else -> null
    }

    private companion object {
        const val REVEALING_FOUNDATION = 110
        const val FOUNDATION = 100
        const val REVEALS_CARD = 90
        const val EMPTIES_COLUMN = 70
        const val WASTE_TO_TABLEAU = 60
        const val DRAW = 10
        const val RECYCLE = 5
        const val USELESS = 0
    }
}
