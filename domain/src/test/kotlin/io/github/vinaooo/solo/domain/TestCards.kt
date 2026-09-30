package io.github.vinaooo.solo.domain

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit

/** Parses short notation: "AS", "10H", "QD", "KC". */
fun card(notation: String, faceUp: Boolean = true): Card {
    val suit = when (notation.last()) {
        'C' -> Suit.CLUBS
        'D' -> Suit.DIAMONDS
        'H' -> Suit.HEARTS
        'S' -> Suit.SPADES
        else -> error("Unknown suit in $notation")
    }
    val rank = when (val r = notation.dropLast(1)) {
        "A" -> Rank.ACE
        "J" -> Rank.JACK
        "Q" -> Rank.QUEEN
        "K" -> Rank.KING
        else -> Rank.entries.first { it.value == r.toInt() }
    }
    return Card(suit, rank, faceUp)
}

fun up(vararg notations: String): List<Card> = notations.map { card(it, faceUp = true) }

fun down(vararg notations: String): List<Card> = notations.map { card(it, faceUp = false) }

/** Full suit from ace up to [upTo], face up — handy for foundations. */
fun suitRun(suit: Char, upTo: Int = 13): List<Card> = (1..upTo).map { value ->
    val rank = when (value) {
        1 -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> value.toString()
    }
    card("$rank$suit")
}

fun emptyState(drawMode: DrawMode = DrawMode.ONE) = GameState(
    stock = emptyList(),
    waste = emptyList(),
    foundations = List(GameState.FOUNDATION_COUNT) { emptyList() },
    tableau = List(GameState.TABLEAU_COUNT) { emptyList() },
    drawMode = drawMode,
)

fun GameState.withTableau(index: Int, cards: List<Card>) =
    copy(tableau = tableau.toMutableList().also { it[index] = cards })

fun GameState.withFoundation(index: Int, cards: List<Card>) =
    copy(foundations = foundations.toMutableList().also { it[index] = cards })
