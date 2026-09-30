package io.github.vinaooo.solo.domain.model

enum class SuitColor { RED, BLACK }

enum class Suit(val color: SuitColor) {
    CLUBS(SuitColor.BLACK),
    DIAMONDS(SuitColor.RED),
    HEARTS(SuitColor.RED),
    SPADES(SuitColor.BLACK),
}
