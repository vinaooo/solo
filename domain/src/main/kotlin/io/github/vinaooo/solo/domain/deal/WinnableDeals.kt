package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode

/**
 * The seeds of deals a solver has won, generated once by `WinnableDealsGenerator` (a test) and shipped as resources:
 * one list per draw mode, pass limit and difficulty, since each changes which deals can be won.
 */
object WinnableDeals {

    /** Easy deals were won within this many positions searched, Normal ones within [NORMAL_POSITIONS]. */
    const val EASY_POSITIONS = 1_000
    const val NORMAL_POSITIONS = 20_000

    private val lists = HashMap<String, LongArray>()

    /** The seed at [index] of the list for these rules and [difficulty] (Easy or Normal), wrapping around. */
    fun seedAt(index: Long, drawMode: DrawMode, mode: GameMode, difficulty: Difficulty): Long {
        val seeds = seeds(drawMode, mode.recycleLimit(drawMode) != null, difficulty)
        return seeds[index.mod(seeds.size)]
    }

    fun seeds(drawMode: DrawMode, limitedPasses: Boolean, difficulty: Difficulty): LongArray {
        val name = fileName(drawMode, limitedPasses, difficulty)
        return synchronized(lists) { lists.getOrPut(name) { load(name) } }
    }

    fun fileName(drawMode: DrawMode, limitedPasses: Boolean, difficulty: Difficulty): String =
        "${drawMode.name}-${if (limitedPasses) "limited" else "unlimited"}-${difficulty.name}.txt".lowercase()

    private fun load(name: String): LongArray {
        val stream = checkNotNull(javaClass.getResourceAsStream("/deals/$name")) { "Missing deal list $name" }
        return stream.bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map(String::toLong).toList().toLongArray()
        }
    }
}
