package io.github.vinaooo.solo.domain.stats

import io.github.vinaooo.solo.domain.model.GameStats
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GameStatsTest {

    @Test
    fun `a win counts the game, the win and extends the streak`() {
        val stats = GameStats(played = 4, won = 2, currentStreak = 2, bestStreak = 2).afterWin()

        stats shouldBe GameStats(played = 5, won = 3, currentStreak = 3, bestStreak = 3)
    }

    @Test
    fun `a loss counts the game and resets the streak but keeps the best`() {
        val stats = GameStats(played = 4, won = 3, currentStreak = 3, bestStreak = 3).afterLoss()

        stats shouldBe GameStats(played = 5, won = 3, currentStreak = 0, bestStreak = 3)
    }

    @Test
    fun `best streak only grows when the current streak passes it`() {
        GameStats(played = 9, won = 6, currentStreak = 1, bestStreak = 5).afterWin().bestStreak shouldBe 5
    }

    @Test
    fun `win rate is a percentage, zero with no games`() {
        GameStats().winRatePercent shouldBe 0
        GameStats(played = 3, won = 1).winRatePercent shouldBe 33
        GameStats(played = 3, won = 2).winRatePercent shouldBe 67
    }
}
