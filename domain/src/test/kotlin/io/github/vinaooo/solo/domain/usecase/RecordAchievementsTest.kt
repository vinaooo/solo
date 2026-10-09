package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.fake.FakeAchievementRepository
import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.model.Achievement
import io.github.vinaooo.solo.domain.model.Achievements
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.badges
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RecordAchievementsTest {

    private val badges = FakeAchievementRepository()
    private val stats = FakeStatsRepository()
    private val settings = FakeSettingsRepository()
    private var today = LocalDate.of(2026, 10, 9)
    private val record = RecordAchievements(
        badges,
        stats,
        settings,
        clock = object : Clock {
            override fun nowMillis() = 0L

            override fun today() = this@RecordAchievementsTest.today
        },
    )

    @Test
    fun `playing marks the day and earns the first day`() = runTest {
        record.played()

        badges.current.value.collected[Achievements.DAYS_PLAYED] shouldBe setOf("2026-10-09")
        badges.current.value.badges shouldBe setOf(Achievement.DAYS_1)
    }

    @Test
    fun `only the first move of a day is recorded`() = runTest {
        record.played()
        badges.current.value = AchievementProgress()

        record.played()
        badges.current.value shouldBe AchievementProgress()

        today = today.plusDays(1)
        record.played()
        badges.current.value.collected[Achievements.DAYS_PLAYED] shouldBe setOf("2026-10-10")
    }

    @Test
    fun `three days in a row earn the three-day badge`() = runTest {
        badges.current.value =
            AchievementProgress(collected = mapOf(Achievements.DAYS_PLAYED to setOf("2026-10-07", "2026-10-08")))

        record.played()

        badges.current.value.badges shouldContainAll setOf(Achievement.DAYS_3)
    }

    @Test
    fun `ladders sum every mode's stats, so earlier games count`() = runTest {
        stats.stats.value = mapOf(
            "STANDARD" to GameStats(played = 6, won = 6),
            "VEGAS" to GameStats(played = 4, won = 4),
        )

        record.played()

        badges.current.value.badges shouldContainAll
            setOf(Achievement.PLAYED_10, Achievement.WON_10, Achievement.WIN_STANDARD, Achievement.WIN_VEGAS)
    }

    @Test
    fun `wins in any mode add to the streak, a loss ends it`() = runTest {
        val win = GameSession(1, emptyState().copy(foundations = List(4) { suitRun("CDHS"[it]) }))
        settings.current.value = Settings(winStreak = 2)

        record.gameEnded(win)

        settings.current.value.winStreak shouldBe 3
        badges.current.value.badges shouldContainAll setOf(Achievement.STREAK_3)

        record.gameEnded(GameSession(2, emptyState().copy(moves = 3)))

        settings.current.value.winStreak shouldBe 0
    }
}
