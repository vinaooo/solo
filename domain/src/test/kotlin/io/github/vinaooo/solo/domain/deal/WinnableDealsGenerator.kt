package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/**
 * Writes the deal lists [WinnableDeals] reads: `./gradlew :domain:generateDeals` (about half an hour). Run it again if
 * Kotlin's seeded random ever changes, which `WinnableDealsTest` catches. It runs outside the test task: run as a
 * test, in parallel, the search was about 50 times slower, likely because of the coverage agent.
 */
object WinnableDealsGenerator {

    private const val LIST = 2_000

    /** One pass in Draw 1 wins so few deals that a shorter list keeps the search to minutes. */
    private const val SHORT_LIST = 1_000

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking(Dispatchers.Default) {
        val dir = File(args.single()).apply { mkdirs() }
        dealLists().map { (drawMode, limitedPasses, difficulty) ->
            async {
                val count = if (drawMode == DrawMode.ONE && limitedPasses) SHORT_LIST else LIST
                val seeds = generateSequence(1L) { it + 1 }
                    .filter { isWinnable(it, drawMode, limitedPasses, difficulty) }
                    .take(count)
                    .toList()
                File(dir, WinnableDeals.fileName(drawMode, limitedPasses, difficulty))
                    .writeText(seeds.joinToString("\n", postfix = "\n"))
            }
        }.awaitAll()
    }
}

/** Every list: each draw mode, with unlimited or limited passes, at Easy and Normal. */
internal fun dealLists(): List<Triple<DrawMode, Boolean, Difficulty>> = DrawMode.entries.flatMap { drawMode ->
    listOf(false, true).flatMap { limitedPasses ->
        listOf(Difficulty.EASY, Difficulty.NORMAL).map { Triple(drawMode, limitedPasses, it) }
    }
}

/**
 * Whether the deal of [seed] belongs to [difficulty]'s list, under the given rules: Easy if the solver wins it within
 * [WinnableDeals.EASY_POSITIONS], Normal if it needs more than that but wins within [WinnableDeals.NORMAL_POSITIONS],
 * so the two lists share no deal.
 */
internal fun isWinnable(seed: Long, drawMode: DrawMode, limitedPasses: Boolean, difficulty: Difficulty): Boolean {
    val mode = if (limitedPasses) GameMode.VEGAS else GameMode.STANDARD
    val deal = Dealer().deal(SeededShuffler(seed), drawMode).copy(mode = mode)
    val budget = if (difficulty == Difficulty.EASY) WinnableDeals.EASY_POSITIONS else WinnableDeals.NORMAL_POSITIONS
    val positions = runBlocking { DealSolver().positionsToWin(deal, budget) } ?: return false
    return difficulty == Difficulty.EASY || positions > WinnableDeals.EASY_POSITIONS
}
