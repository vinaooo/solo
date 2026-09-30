package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef

/** Where each card that can be picked up (the waste and foundation tops, face-up tableau cards) can legally go. */
internal fun MoveResolver.destinationsOf(board: GameState): Map<CardSpot, List<PileRef>> = buildMap {
    fun offer(pile: PileRef, index: Int) {
        destinations(board, pile, index).takeIf { it.isNotEmpty() }?.let { put(CardSpot(pile, index), it) }
    }
    if (board.waste.isNotEmpty()) offer(PileRef.Waste, board.waste.lastIndex)
    board.foundations.forEachIndexed { f, pile -> if (pile.isNotEmpty()) offer(PileRef.Foundation(f), pile.lastIndex) }
    board.tableau.forEachIndexed { column, pile ->
        pile.forEachIndexed { i, card -> if (card.isFaceUp) offer(PileRef.Tableau(column), i) }
    }
}

internal fun GameState.hasSamePilesAs(other: GameState): Boolean = stock == other.stock &&
    waste == other.waste &&
    foundations == other.foundations &&
    tableau == other.tableau
