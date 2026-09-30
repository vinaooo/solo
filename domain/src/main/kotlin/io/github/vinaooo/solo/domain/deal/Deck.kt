package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit

object Deck {
    fun standard(): List<Card> = Suit.entries.flatMap { suit -> Rank.entries.map { rank -> Card(suit, rank) } }
}
