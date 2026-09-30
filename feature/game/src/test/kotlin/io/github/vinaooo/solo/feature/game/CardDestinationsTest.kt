package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardDestinationsTest {

    private val board = GameState(
        stock = listOf(Card(Suit.CLUBS, Rank.KING)),
        waste = listOf(Card(Suit.HEARTS, Rank.SIX, isFaceUp = true), Card(Suit.DIAMONDS, Rank.TWO, isFaceUp = true)),
        foundations = listOf(listOf(Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true))) + List(3) { emptyList() },
        tableau = List(GameState.TABLEAU_COUNT) { column ->
            when (column) {
                0 -> listOf(Card(Suit.SPADES, Rank.THREE, isFaceUp = true))
                1 -> listOf(Card(Suit.CLUBS, Rank.FOUR), Card(Suit.HEARTS, Rank.SEVEN, isFaceUp = true))
                2 -> listOf(Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true))
                else -> listOf(Card(Suit.SPADES, Rank.NINE, isFaceUp = true))
            }
        },
        drawMode = DrawMode.ONE,
    )

    @Test
    fun `offers the waste top, foundation tops and face-up column cards that have somewhere to go`() {
        MoveResolver().destinationsOf(board) shouldBe mapOf(
            CardSpot(PileRef.Waste, 1) to listOf(PileRef.Foundation(0), PileRef.Tableau(0)),
            CardSpot(PileRef.Tableau(1), 1) to listOf(PileRef.Tableau(2)),
        )
    }

    @Test
    fun `boards with the same cards in the same places match whatever the clock says`() {
        board.hasSamePilesAs(board.copy(elapsedSeconds = 40, score = 12)).shouldBeTrue()
        board.hasSamePilesAs(board.copy(stock = emptyList())).shouldBeFalse()
        board.hasSamePilesAs(board.copy(waste = emptyList())).shouldBeFalse()
        board.hasSamePilesAs(board.copy(foundations = List(4) { emptyList() })).shouldBeFalse()
        board.hasSamePilesAs(board.copy(tableau = board.tableau.reversed())).shouldBeFalse()
    }
}
