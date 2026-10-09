package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.vinkit.core.AchievementProgress
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.junit.jupiter.api.Test

class AchievementsTest {

    private val none = AchievementFacts(played = 0, won = 0, modesWon = emptySet(), winStreak = 0, dayStreak = 0)

    private val won = emptyState()
        .copy(moves = 100, elapsedSeconds = 180, difficulty = Difficulty.NORMAL)
        .withFoundation(0, suitRun('C'))
        .withFoundation(1, suitRun('D'))
        .withFoundation(2, suitRun('H'))
        .withFoundation(3, suitRun('S'))

    private fun earned(facts: AchievementFacts) = Achievements.earned(facts)

    /** Each rung is earned at its count and not one below, so `>=` can't drift to `>`. */
    private fun checkLadder(rungs: Map<Int, Achievement>, facts: (Int) -> AchievementFacts) {
        rungs.forEach { (count, badge) ->
            earned(facts(count - 1)) shouldNotContain badge
            earned(facts(count)) shouldContain badge
        }
    }

    @Test
    fun `nothing played earns nothing`() {
        earned(none).shouldBeEmpty()
    }

    @Test
    fun `games played ladder`() = checkLadder(
        mapOf(
            1 to Achievement.PLAYED_1,
            10 to Achievement.PLAYED_10,
            50 to Achievement.PLAYED_50,
            100 to Achievement.PLAYED_100,
            500 to Achievement.PLAYED_500,
        ),
    ) { none.copy(played = it) }

    @Test
    fun `days in a row ladder`() = checkLadder(
        mapOf(
            1 to Achievement.DAYS_1,
            3 to Achievement.DAYS_3,
            5 to Achievement.DAYS_5,
            10 to Achievement.DAYS_10,
            20 to Achievement.DAYS_20,
            30 to Achievement.DAYS_30,
            50 to Achievement.DAYS_50,
            100 to Achievement.DAYS_100,
            200 to Achievement.DAYS_200,
        ),
    ) { none.copy(dayStreak = it) }

    @Test
    fun `wins ladder`() = checkLadder(
        mapOf(
            1 to Achievement.WON_1,
            10 to Achievement.WON_10,
            50 to Achievement.WON_50,
            100 to Achievement.WON_100,
            500 to Achievement.WON_500,
            1000 to Achievement.WON_1000,
        ),
    ) { none.copy(won = it) }

    @Test
    fun `win streak ladder`() = checkLadder(
        mapOf(
            3 to Achievement.STREAK_3,
            5 to Achievement.STREAK_5,
            10 to Achievement.STREAK_10,
            20 to Achievement.STREAK_20,
        ),
    ) { none.copy(winStreak = it) }

    @Test
    fun `a mode won earns its badge, all four earn every mode`() {
        earned(none.copy(modesWon = setOf(GameMode.VEGAS))) shouldContainExactlyInAnyOrder listOf(Achievement.WIN_VEGAS)
        val all = earned(none.copy(modesWon = GameMode.entries.toSet()))
        all shouldContainExactlyInAnyOrder listOf(
            Achievement.WIN_STANDARD,
            Achievement.WIN_VEGAS,
            Achievement.WIN_VEGAS_CUMULATIVE,
            Achievement.WIN_COUNTER_TIME,
            Achievement.WIN_EVERY_MODE,
        )
    }

    @Test
    fun `a win just over the limits earns no skill badge`() {
        earned(none.copy(ended = GameSession(1, won, undos = 1))).shouldBeEmpty()
    }

    @Test
    fun `a quick, short, clean Draw 3 win on Hard earns every skill badge`() {
        val state = won.copy(moves = 99, elapsedSeconds = 179, drawMode = DrawMode.THREE, difficulty = Difficulty.HARD)

        earned(none.copy(ended = GameSession(1, state))) shouldContainExactlyInAnyOrder listOf(
            Achievement.WIN_DRAW_3,
            Achievement.WIN_HARD,
            Achievement.WIN_NO_UNDO,
            Achievement.WIN_UNDER_3_MIN,
            Achievement.WIN_UNDER_100_MOVES,
        )
    }

    @Test
    fun `a lost game earns no win badge`() {
        val lost = emptyState().copy(moves = 5, drawMode = DrawMode.THREE, difficulty = Difficulty.HARD)

        earned(none.copy(ended = GameSession(1, lost))).shouldBeEmpty()
    }

    @Test
    fun `Vegas profit needs 11 cards on the foundations, in either Vegas`() {
        val ten = emptyState().copy(mode = GameMode.VEGAS).withFoundation(0, suitRun('C', upTo = 10))
        val eleven = ten.withFoundation(1, suitRun('D', upTo = 1))

        earned(none.copy(ended = GameSession(1, ten))) shouldNotContain Achievement.VEGAS_PROFIT
        earned(none.copy(ended = GameSession(1, eleven))) shouldContain Achievement.VEGAS_PROFIT
        val cumulative = eleven.copy(mode = GameMode.VEGAS_CUMULATIVE, score = -100)
        earned(none.copy(ended = GameSession(1, cumulative))) shouldContain Achievement.VEGAS_PROFIT
        val standard = eleven.copy(mode = GameMode.STANDARD)
        earned(none.copy(ended = GameSession(1, standard))) shouldNotContain Achievement.VEGAS_PROFIT
    }

    @Test
    fun `a cumulative game ending above zero earns the bank badge`() {
        val cumulative = emptyState().copy(mode = GameMode.VEGAS_CUMULATIVE)

        earned(none.copy(ended = GameSession(1, cumulative.copy(score = 0)))) shouldNotContain Achievement.BANK_POSITIVE
        earned(none.copy(ended = GameSession(1, cumulative.copy(score = 1)))) shouldContain Achievement.BANK_POSITIVE
        val vegas = cumulative.copy(mode = GameMode.VEGAS, score = 1)
        earned(none.copy(ended = GameSession(1, vegas))) shouldNotContain Achievement.BANK_POSITIVE
    }

    @Test
    fun `day streak counts back from today and stops at a gap`() {
        val today = LocalDate.of(2026, 1, 2)
        val days = setOf("2026-01-02", "2026-01-01", "2025-12-31", "2025-12-29")

        Achievements.dayStreak(days, today) shouldBe 3
        Achievements.dayStreak(days, LocalDate.of(2026, 1, 3)) shouldBe 0
    }

    @Test
    fun `badges a newer version wrote are kept, but not read as known`() {
        val progress = AchievementProgress(unlocked = setOf("FUTURE"))

        val after = Achievements.after(progress, none.copy(played = 1))

        after.unlocked shouldBe setOf("FUTURE", "PLAYED_1")
        after.badges shouldBe setOf(Achievement.PLAYED_1)
    }
}
