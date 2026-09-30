package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move

class KlondikeRules : RuleSet {

    override fun isLegal(state: GameState, move: Move): Boolean = when (move) {
        is Move.Draw -> DrawRule.isLegal(state, move)
        is Move.Recycle -> RecycleRule.isLegal(state, move)
        is Move.WasteToTableau -> WasteToTableauRule.isLegal(state, move)
        is Move.WasteToFoundation -> WasteToFoundationRule.isLegal(state, move)
        is Move.TableauToTableau -> TableauToTableauRule.isLegal(state, move)
        is Move.TableauToFoundation -> TableauToFoundationRule.isLegal(state, move)
        is Move.FoundationToTableau -> FoundationToTableauRule.isLegal(state, move)
    }

    override fun perform(state: GameState, move: Move): Transition = when (move) {
        is Move.Draw -> DrawRule.perform(state, move)
        is Move.Recycle -> RecycleRule.perform(state, move)
        is Move.WasteToTableau -> WasteToTableauRule.perform(state, move)
        is Move.WasteToFoundation -> WasteToFoundationRule.perform(state, move)
        is Move.TableauToTableau -> TableauToTableauRule.perform(state, move)
        is Move.TableauToFoundation -> TableauToFoundationRule.perform(state, move)
        is Move.FoundationToTableau -> FoundationToTableauRule.perform(state, move)
    }

    override fun legalMoves(state: GameState): List<Move> = candidateMoves(state).filter { isLegal(state, it) }

    private fun candidateMoves(state: GameState): List<Move> = buildList {
        add(Move.Draw)
        add(Move.Recycle)
        foundations.forEach { add(Move.WasteToFoundation(it)) }
        tableau.forEach { add(Move.WasteToTableau(it)) }
        for (from in tableau) {
            foundations.forEach { add(Move.TableauToFoundation(from, it)) }
            for (to in tableau) {
                state.tableau[from].indices.forEach { add(Move.TableauToTableau(from, to, count = it + 1)) }
            }
        }
        for (from in foundations) tableau.forEach { add(Move.FoundationToTableau(from, it)) }
    }

    private companion object {
        val tableau = 0 until GameState.TABLEAU_COUNT
        val foundations = 0 until GameState.FOUNDATION_COUNT
    }
}
