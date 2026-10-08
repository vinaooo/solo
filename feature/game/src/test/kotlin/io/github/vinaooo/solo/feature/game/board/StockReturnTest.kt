package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

class StockReturnTest {

    @Test
    fun `a card going back to the stock slides under it until it settles`() {
        val stockReturn = StockReturn(PileRef.Stock).apply { start() }

        stockReturn.slidesUnder(PileRef.Stock).shouldBeTrue()
        stockReturn.settled()
        stockReturn.slidesUnder(PileRef.Stock).shouldBeFalse()
    }

    @Test
    fun `drawn again before its return settles, it does not slide under the waste`() {
        // The return's animation was cut short, so it never settled.
        val stockReturn = StockReturn(PileRef.Stock).apply { start() }

        stockReturn.slidesUnder(PileRef.Waste).shouldBeFalse()
    }

    @Test
    fun `a card launched by a later move flies above one still in the air, whatever their destinations`() {
        val deepColumn = PlacedCard(Card(Suit.HEARTS, Rank.QUEEN), PileRef.Tableau(6), 12, Position(0f, 0f), z = 312f)
        val foundation = PlacedCard(Card(Suit.CLUBS, Rank.TWO), PileRef.Foundation(0), 1, Position(0f, 0f), z = 201f)
        val returning = StockReturn(PileRef.Waste)

        val earlier = returning.zIndex(deepColumn, flying = true, launch = 4)
        val later = returning.zIndex(foundation, flying = true, launch = 5)

        (later > earlier).shouldBeTrue()
        (earlier > deepColumn.z).shouldBeTrue()
    }
}
