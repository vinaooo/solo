package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardRoleTest {

    private val fiveOfSpades = Card(Suit.SPADES, Rank.FIVE)
    private val nineOfHearts = Card(Suit.HEARTS, Rank.NINE, isFaceUp = true)
    private val eightOfClubs = Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true)
    private val sevenOfClubs = Card(Suit.CLUBS, Rank.SEVEN, isFaceUp = true)
    private val aceOfHearts = Card(Suit.HEARTS, Rank.ACE, isFaceUp = true)
    private val twoOfHearts = Card(Suit.HEARTS, Rank.TWO, isFaceUp = true)
    private val tenOfSpades = Card(Suit.SPADES, Rank.TEN, isFaceUp = true)

    private val state = GameState(
        stock = listOf(Card(Suit.DIAMONDS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN)),
        waste = listOf(Card(Suit.CLUBS, Rank.SIX, isFaceUp = true), sevenOfClubs),
        foundations = listOf(listOf(aceOfHearts, twoOfHearts)) + List(3) { emptyList() },
        tableau = List(GameState.TABLEAU_COUNT) { column ->
            when (column) {
                0 -> listOf(fiveOfSpades, nineOfHearts, eightOfClubs)
                1 -> listOf(tenOfSpades)
                else -> emptyList()
            }
        },
        drawMode = DrawMode.ONE,
    )
    private val placed = BoardLayout(width = 1_000f, height = 2_000f, gap = 4f).positions(state)

    private fun roleOf(card: Card) = cardRole(state, placed.getValue(card.identity()))

    @Test
    fun `the top of the stock says how many cards it holds, the cards under it are skipped`() {
        roleOf(Card(Suit.DIAMONDS, Rank.QUEEN)) shouldBe CardRole.StockTop(count = 2)
        roleOf(Card(Suit.DIAMONDS, Rank.KING)) shouldBe CardRole.Hidden
    }

    @Test
    fun `only the top card of the waste and of a foundation is read`() {
        roleOf(sevenOfClubs) shouldBe CardRole.WasteTop(sevenOfClubs)
        roleOf(Card(Suit.CLUBS, Rank.SIX)) shouldBe CardRole.Hidden
        roleOf(twoOfHearts) shouldBe CardRole.FoundationTop(twoOfHearts)
        roleOf(aceOfHearts) shouldBe CardRole.Hidden
    }

    @Test
    fun `face-down cards are counted by the first face-up card of their column`() {
        roleOf(fiveOfSpades) shouldBe CardRole.Hidden
        roleOf(nineOfHearts) shouldBe CardRole.InColumn(nineOfHearts, column = 0, faceDownBelow = 1)
        roleOf(eightOfClubs) shouldBe CardRole.InColumn(eightOfClubs, column = 0, faceDownBelow = 0)
        roleOf(tenOfSpades) shouldBe CardRole.InColumn(tenOfSpades, column = 1, faceDownBelow = 0)
    }

    @Test
    fun `left-handed, TalkBack reads stock, waste and foundations, then each column from top to bottom`() {
        readingOrder(Handedness.LEFT, PileRef.Stock, PileRef.Waste, PileRef.Foundation(0), PileRef.Foundation(3))
    }

    @Test
    fun `right-handed, TalkBack reads foundations, waste and stock, then each column from top to bottom`() {
        readingOrder(Handedness.RIGHT, PileRef.Foundation(0), PileRef.Foundation(3), PileRef.Waste, PileRef.Stock)
    }

    @Test
    fun `sideways, TalkBack reads the stock before the waste under it on either hand`() {
        val foundations = arrayOf(PileRef.Foundation(0), PileRef.Foundation(3))
        readingOrder(Handedness.LEFT, PileRef.Stock, PileRef.Waste, *foundations, sideways = true)
        readingOrder(Handedness.RIGHT, *foundations, PileRef.Stock, PileRef.Waste, sideways = true)
    }

    private fun readingOrder(handedness: Handedness, vararg topRow: PileRef, sideways: Boolean = false) {
        val columns = listOf(
            PileRef.Tableau(0) to 0,
            PileRef.Tableau(0) to 18,
            PileRef.Tableau(1) to 0,
            PileRef.Tableau(6) to 18,
        )
        val order = (topRow.map { it to 0 } + columns).map { (pile, index) ->
            traversalOrder(pile, index, handedness, sideways)
        }

        order shouldBeSortedWith naturalOrder()
        order.toSet().size shouldBe order.size
    }
}
