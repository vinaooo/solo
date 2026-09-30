package io.github.vinaooo.solo.domain.interaction

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.rules.KlondikeRules
import io.github.vinaooo.solo.domain.rules.RuleSet

/** Translates taps (best destination) and drops (explicit destination) into legal moves. */
class MoveResolver(private val rules: RuleSet = KlondikeRules()) {

    fun resolveTap(state: GameState, pile: PileRef, cardIndex: Int): Move? =
        tapCandidates(state, pile, cardIndex).firstOrNull { rules.isLegal(state, it) }

    fun resolveDrop(state: GameState, from: PileRef, cardIndex: Int, to: PileRef): Move? =
        dropMove(state, from, cardIndex, to)?.takeIf { rules.isLegal(state, it) }

    /**
     * Every pile the card at [cardIndex] (with the cards on top of it) can legally move to, foundations first.
     * It lets a player pick the destination without dragging, for example from TalkBack's actions menu.
     */
    fun destinations(state: GameState, from: PileRef, cardIndex: Int): List<PileRef> {
        if (!canPickUp(state, from, cardIndex)) return emptyList()
        return targets.filter { to ->
            to != from &&
                !isWholeColumnToEmpty(state, from, cardIndex, to) &&
                resolveDrop(state, from, cardIndex, to) != null
        }
    }

    private fun canPickUp(state: GameState, from: PileRef, cardIndex: Int): Boolean = when (from) {
        PileRef.Stock -> false
        PileRef.Waste -> cardIndex == state.waste.lastIndex
        is PileRef.Foundation -> cardIndex == state.foundations[from.index].lastIndex
        is PileRef.Tableau -> cardIndex in state.tableau[from.index].indices
    }

    // Moving a whole column into an empty one changes nothing.
    private fun isWholeColumnToEmpty(state: GameState, from: PileRef, cardIndex: Int, to: PileRef): Boolean =
        from is PileRef.Tableau && cardIndex == 0 && to is PileRef.Tableau && state.tableau[to.index].isEmpty()

    private fun tapCandidates(state: GameState, pile: PileRef, cardIndex: Int): List<Move> = when (pile) {
        PileRef.Stock -> listOf(Move.Draw, Move.Recycle)
        PileRef.Waste -> foundationIndexes.map { Move.WasteToFoundation(it) } +
            tableauIndexes.map { Move.WasteToTableau(it) }
        is PileRef.Foundation -> tableauIndexes.map { Move.FoundationToTableau(pile.index, it) }
        is PileRef.Tableau -> tableauTapCandidates(state, pile.index, cardIndex)
    }

    private fun tableauTapCandidates(state: GameState, column: Int, cardIndex: Int): List<Move> {
        val pile = state.tableau.getOrNull(column) ?: return emptyList()
        if (cardIndex !in pile.indices) return emptyList()
        val count = pile.size - cardIndex
        val toFoundation = if (count ==
            1
        ) {
            foundationIndexes.map { Move.TableauToFoundation(column, it) }
        } else {
            emptyList()
        }
        // Moving a whole column into an empty one changes nothing.
        val targets = tableauIndexes.filterNot { cardIndex == 0 && state.tableau[it].isEmpty() }
        return toFoundation + targets.map { Move.TableauToTableau(column, it, count) }
    }

    private fun dropMove(state: GameState, from: PileRef, cardIndex: Int, to: PileRef): Move? = when (from) {
        PileRef.Waste -> when (to) {
            is PileRef.Tableau -> Move.WasteToTableau(to.index)
            is PileRef.Foundation -> Move.WasteToFoundation(to.index)
            else -> null
        }
        is PileRef.Tableau -> when (to) {
            is PileRef.Tableau -> Move.TableauToTableau(
                from.index,
                to.index,
                state.tableau[from.index].size - cardIndex,
            )
            is PileRef.Foundation -> Move.TableauToFoundation(from.index, to.index)
                .takeIf { cardIndex == state.tableau[from.index].lastIndex }
            else -> null
        }
        is PileRef.Foundation -> (to as? PileRef.Tableau)?.let { Move.FoundationToTableau(from.index, it.index) }
        PileRef.Stock -> null
    }

    private companion object {
        val tableauIndexes = 0 until GameState.TABLEAU_COUNT
        val foundationIndexes = 0 until GameState.FOUNDATION_COUNT
        val targets: List<PileRef> = foundationIndexes.map { PileRef.Foundation(it) } +
            tableauIndexes.map { PileRef.Tableau(it) }
    }
}
