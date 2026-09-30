package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef

/**
 * How TalkBack presents a card. Only the cards a player can act on or needs to know are exposed: the top of the
 * stock, waste and foundations, and the face-up tableau cards. Face-down tableau cards are counted by the first
 * face-up card of their column instead of each being a stop of its own.
 */
internal sealed interface CardRole {
    data object Hidden : CardRole

    data class StockTop(val count: Int) : CardRole

    data class WasteTop(val card: Card) : CardRole

    data class FoundationTop(val card: Card) : CardRole

    /** [faceDownBelow] is set on the first face-up card of a column, and 0 on the cards above it. */
    data class InColumn(val card: Card, val column: Int, val faceDownBelow: Int) : CardRole
}

internal fun cardRole(state: GameState, placed: PlacedCard): CardRole {
    val pile = placed.pile
    val index = placed.index
    return when {
        pile == PileRef.Stock -> if (index == state.stock.lastIndex) CardRole.StockTop(state.stock.size) else null
        pile == PileRef.Waste -> CardRole.WasteTop(placed.card).takeIf { index == state.waste.lastIndex }
        pile is PileRef.Foundation ->
            CardRole.FoundationTop(placed.card).takeIf { index == state.foundations[pile.index].lastIndex }
        pile is PileRef.Tableau && placed.card.isFaceUp -> {
            val faceDown = state.tableau[pile.index].count { !it.isFaceUp }
            CardRole.InColumn(placed.card, pile.index, if (index == faceDown) faceDown else 0)
        }
        else -> null
    } ?: CardRole.Hidden
}

/**
 * TalkBack reading order: the stock, the waste and the foundations, then each column from top to bottom. Without
 * it, cards would be read row by row across the columns.
 */
internal fun traversalOrder(pile: PileRef, index: Int): Float = when (pile) {
    PileRef.Stock -> 0f
    PileRef.Waste -> 1f
    is PileRef.Foundation -> FIRST_FOUNDATION_ORDER + pile.index
    is PileRef.Tableau -> FIRST_COLUMN_ORDER + pile.index * COLUMN_ORDER_SPAN + index
}

private const val FIRST_FOUNDATION_ORDER = 2f
private const val FIRST_COLUMN_ORDER = 10f
private const val COLUMN_ORDER_SPAN = 100f
