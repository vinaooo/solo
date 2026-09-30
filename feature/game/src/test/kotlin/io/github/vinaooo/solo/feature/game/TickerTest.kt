package io.github.vinaooo.solo.feature.game

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TickerTest {

    @Test
    fun `ticks once per period while started`() = runTest {
        var ticks = 0
        val ticker = Ticker(backgroundScope, PERIOD) { ticks++ }

        ticker.start()
        advance(PERIOD * 3)

        ticks shouldBe 3
    }

    @Test
    fun `does not tick before the first period ends`() = runTest {
        var ticks = 0
        val ticker = Ticker(backgroundScope, PERIOD) { ticks++ }

        ticker.start()
        advance(PERIOD - 1)

        ticks shouldBe 0
    }

    @Test
    fun `stop halts the ticks`() = runTest {
        var ticks = 0
        val ticker = Ticker(backgroundScope, PERIOD) { ticks++ }

        ticker.start()
        advance(PERIOD)
        ticker.stop()
        advance(PERIOD * 5)

        ticks shouldBe 1
    }

    @Test
    fun `starting twice keeps a single loop`() = runTest {
        var ticks = 0
        val ticker = Ticker(backgroundScope, PERIOD) { ticks++ }

        ticker.start()
        ticker.start()
        advance(PERIOD * 2)

        ticks shouldBe 2
    }

    @Test
    fun `it can start again after stopping`() = runTest {
        var ticks = 0
        val ticker = Ticker(backgroundScope, PERIOD) { ticks++ }

        ticker.start()
        ticker.stop()
        ticker.start()
        advance(PERIOD)

        ticks shouldBe 1
    }

    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private companion object {
        const val PERIOD = 1_000L
    }
}
