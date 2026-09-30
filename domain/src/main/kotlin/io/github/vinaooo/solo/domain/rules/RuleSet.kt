package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move

/** The rules of a solitaire variant. New variants add a new [RuleSet] instead of changing the engine. */
interface RuleSet {
    fun isLegal(state: GameState, move: Move): Boolean

    /** Performs a move. Callers must check [isLegal] first. */
    fun perform(state: GameState, move: Move): Transition

    fun legalMoves(state: GameState): List<Move>
}
