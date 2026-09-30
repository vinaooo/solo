package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.Rank

/** Klondike's stacking rules, shared by every move that places cards. */
object PlacementRules {

    fun canStackOnTableau(card: Card, pile: List<Card>): Boolean {
        val top = pile.lastOrNull() ?: return card.rank == Rank.KING
        return top.isFaceUp && card.rank.isOneBelow(top.rank) && card.hasOppositeColorOf(top)
    }

    fun canStackOnFoundation(card: Card, pile: List<Card>): Boolean {
        if (!card.isFaceUp) return false
        val top = pile.lastOrNull() ?: return card.rank == Rank.ACE
        return card.suit == top.suit && top.rank.isOneBelow(card.rank)
    }

    fun isMovableRun(cards: List<Card>): Boolean = cards.isNotEmpty() &&
        cards.all { it.isFaceUp } &&
        cards.zipWithNext().all { (upper, lower) ->
            lower.rank.isOneBelow(upper.rank) && lower.hasOppositeColorOf(upper)
        }
}
