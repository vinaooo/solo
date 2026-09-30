package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DealerTest {

    private val dealer = Dealer()

    @Test
    fun `tableau column i receives i+1 cards with only the last one face up`() {
        val state = dealer.deal(SeededShuffler(42), DrawMode.ONE)

        state.tableau.forEachIndexed { index, pile ->
            pile shouldHaveSize index + 1
            pile.last().isFaceUp shouldBe true
            pile.dropLast(1).none { it.isFaceUp } shouldBe true
        }
    }

    @Test
    fun `remaining 24 cards go face down to the stock, waste and foundations start empty`() {
        val state = dealer.deal(SeededShuffler(42), DrawMode.THREE)

        state.stock shouldHaveSize 24
        state.stock.none { it.isFaceUp } shouldBe true
        state.waste.shouldBeEmpty()
        state.foundations.forEach { it.shouldBeEmpty() }
        state.drawMode shouldBe DrawMode.THREE
        state.score shouldBe 0
        state.moves shouldBe 0
    }

    @Test
    fun `every deal contains all 52 cards exactly once`() = runTest {
        checkAll(Arb.long()) { seed ->
            val cards = dealer.deal(SeededShuffler(seed), DrawMode.ONE).allCards()
            cards shouldHaveSize 52
            cards.map { it.suit to it.rank }.shouldNotContainDuplicates()
        }
    }

    @Test
    fun `same seed deals the same game`() {
        dealer.deal(SeededShuffler(7), DrawMode.ONE) shouldBe dealer.deal(SeededShuffler(7), DrawMode.ONE)
    }

    @Test
    fun `layout constants match Klondike`() {
        GameState.TABLEAU_COUNT shouldBe 7
        GameState.FOUNDATION_COUNT shouldBe 4
    }
}
