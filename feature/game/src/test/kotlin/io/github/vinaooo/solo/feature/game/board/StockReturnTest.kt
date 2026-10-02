package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.domain.model.PileRef
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
}
