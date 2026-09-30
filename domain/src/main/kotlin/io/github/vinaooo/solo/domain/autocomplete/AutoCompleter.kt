package io.github.vinaooo.solo.domain.autocomplete

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.PlacementRules

/**
 * Finishes a game once nothing is hidden. With every card face up, the lowest remaining card is always on top
 * of its column and its foundation is ready for it, so repeating [nextMove] always wins.
 */
class AutoCompleter {

    fun canAutoComplete(state: GameState): Boolean =
        !state.isWon && state.stock.isEmpty() && state.waste.isEmpty() && state.tableau.flatten().all { it.isFaceUp }

    fun nextMove(state: GameState): Move? {
        if (state.isWon) return null
        return state.tableau.indices
            .filter { state.tableau[it].isNotEmpty() }
            .sortedBy { state.tableau[it].last().rank.value }
            .firstNotNullOfOrNull { column -> toFoundation(state, column) }
    }

    private fun toFoundation(state: GameState, column: Int): Move? {
        val card = state.tableau[column].last()
        return state.foundations.indices
            .firstOrNull { PlacementRules.canStackOnFoundation(card, state.foundations[it]) }
            ?.let { Move.TableauToFoundation(column, it) }
    }
}
