package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState

/** Lays out a shuffled deck in the classic Klondike layout. */
class Dealer {
    fun deal(shuffler: Shuffler, drawMode: DrawMode): GameState {
        val deck = shuffler.shuffle(Deck.standard()).map { it.faceDown() }.toMutableList()
        val tableau = List(GameState.TABLEAU_COUNT) { column ->
            val pile = List(column + 1) { deck.removeAt(deck.lastIndex) }
            pile.dropLast(1) + pile.last().faceUp()
        }
        return GameState(
            stock = deck.toList(),
            waste = emptyList(),
            foundations = List(GameState.FOUNDATION_COUNT) { emptyList() },
            tableau = tableau,
            drawMode = drawMode,
        )
    }
}
