package io.github.vinaooo.solo.domain.model

import kotlin.math.roundToInt

/** Lifetime statistics, across every mode, and the cumulative Vegas balance ([vegasBank], in dollars). */
data class GameStats(
    val played: Int = 0,
    val won: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val vegasBank: Int = 0,
) {
    val winRatePercent: Int
        get() = if (played == 0) 0 else (won * PERCENT / played.toDouble()).roundToInt()

    fun afterWin(): GameStats {
        val streak = currentStreak + 1
        return copy(played = played + 1, won = won + 1, currentStreak = streak, bestStreak = maxOf(bestStreak, streak))
    }

    fun afterLoss(): GameStats = copy(played = played + 1, currentStreak = 0)

    private companion object {
        const val PERCENT = 100
    }
}
