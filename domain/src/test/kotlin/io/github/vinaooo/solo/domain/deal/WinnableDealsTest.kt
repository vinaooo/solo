package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WinnableDealsTest {

    private val lists = dealLists()

    @Test
    fun `every list holds distinct seeds, a thousand at least`() {
        lists.forEach { (drawMode, limitedPasses, difficulty) ->
            val seeds = WinnableDeals.seeds(drawMode, limitedPasses, difficulty).toList()
            seeds.size shouldBeGreaterThanOrEqual 1_000
            seeds.shouldBeStrictlyIncreasing()
        }
    }

    @Test
    fun `easy and normal share no deal, so normal is never an easy game`() {
        DrawMode.entries.forEach { drawMode ->
            listOf(false, true).forEach { limitedPasses ->
                val easy = WinnableDeals.seeds(drawMode, limitedPasses, Difficulty.EASY).toSet()
                WinnableDeals.seeds(drawMode, limitedPasses, Difficulty.NORMAL).filter { it in easy }.shouldBeEmpty()
            }
        }
    }

    /** Fails if Kotlin's seeded random changes how a seed shuffles: then the lists must be generated again. */
    @Test
    fun `the listed deals are still won`() {
        lists.forEach { (drawMode, limitedPasses, difficulty) ->
            WinnableDeals.seeds(drawMode, limitedPasses, difficulty).take(SAMPLE).forEach { seed ->
                isWinnable(seed, drawMode, limitedPasses, difficulty) shouldBe true
            }
        }
    }

    @Test
    fun `picks from the list for the mode's passes, any index landing in it`() {
        val vegas = WinnableDeals.seeds(DrawMode.THREE, limitedPasses = true, Difficulty.EASY)
        WinnableDeals.seedAt(1, DrawMode.THREE, GameMode.VEGAS_CUMULATIVE, Difficulty.EASY) shouldBe vegas[1]
        WinnableDeals.seedAt(-1, DrawMode.THREE, GameMode.VEGAS, Difficulty.EASY) shouldBe vegas.last()

        val standard = WinnableDeals.seeds(DrawMode.ONE, limitedPasses = false, Difficulty.NORMAL)
        WinnableDeals.seedAt(standard.size + 2L, DrawMode.ONE, GameMode.COUNTER_TIME, Difficulty.NORMAL) shouldBe
            standard[2]
    }

    @Test
    fun `names each list by draw mode, passes and difficulty`() {
        WinnableDeals.fileName(DrawMode.ONE, limitedPasses = true, Difficulty.EASY) shouldBe "one-limited-easy.txt"
        WinnableDeals.fileName(DrawMode.THREE, limitedPasses = false, Difficulty.NORMAL) shouldBe
            "three-unlimited-normal.txt"
    }

    private companion object {
        const val SAMPLE = 3
    }
}
