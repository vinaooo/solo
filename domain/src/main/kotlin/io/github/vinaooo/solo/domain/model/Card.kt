package io.github.vinaooo.solo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(val suit: Suit, val rank: Rank, val isFaceUp: Boolean = false) {
    fun faceUp(): Card = copy(isFaceUp = true)

    fun faceDown(): Card = copy(isFaceUp = false)

    fun hasOppositeColorOf(other: Card): Boolean = suit.color != other.suit.color
}
