package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.scoring.ScoreEvent

internal fun List<List<Card>>.replace(index: Int, pile: List<Card>): List<List<Card>> =
    toMutableList().also { it[index] = pile }

internal fun GameState.withTableauPile(index: Int, pile: List<Card>) = copy(tableau = tableau.replace(index, pile))

internal fun GameState.withFoundationPile(index: Int, pile: List<Card>) =
    copy(foundations = foundations.replace(index, pile))

internal fun Int.isTableauIndex() = this in 0 until GameState.TABLEAU_COUNT

internal fun Int.isFoundationIndex() = this in 0 until GameState.FOUNDATION_COUNT

/** Removes the top [count] cards from a tableau column and turns the new top card face up. */
internal fun GameState.takeFromTableau(index: Int, count: Int): Pair<Transition, List<Card>> {
    val pile = tableau[index]
    val moving = pile.takeLast(count)
    val remaining = pile.dropLast(count)
    val top = remaining.lastOrNull()
    return if (top != null && !top.isFaceUp) {
        Transition(withTableauPile(index, remaining.dropLast(1) + top.faceUp()), listOf(ScoreEvent.CardRevealed))
    } else {
        Transition(withTableauPile(index, remaining))
    } to moving
}
