package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.vinkit.core.AchievementProgress
import java.time.LocalDate

/** A badge. Its name is its key in storage (vinkit's `AchievementProgress`): never rename one after release. */
enum class Achievement {
    PLAYED_1,
    PLAYED_10,
    PLAYED_50,
    PLAYED_100,
    PLAYED_500,
    DAYS_1,
    DAYS_3,
    DAYS_5,
    DAYS_10,
    DAYS_20,
    DAYS_30,
    DAYS_50,
    DAYS_100,
    DAYS_200,
    WON_1,
    WON_10,
    WON_50,
    WON_100,
    WON_500,
    WON_1000,
    STREAK_3,
    STREAK_5,
    STREAK_10,
    STREAK_20,
    WIN_STANDARD,
    WIN_VEGAS,
    WIN_VEGAS_CUMULATIVE,
    WIN_COUNTER_TIME,
    WIN_EVERY_MODE,
    WIN_DRAW_3,
    WIN_HARD,
    WIN_NO_UNDO,
    WIN_UNDER_3_MIN,
    WIN_UNDER_100_MOVES,
    VEGAS_PROFIT,
    BANK_POSITIVE,
}

/** The badges earned that this version knows; keys a newer version wrote are skipped. */
val AchievementProgress.badges: Set<Achievement>
    get() = Achievement.entries.filter { it.name in unlocked }.toSet()

/**
 * What the badges are judged on: games [played] and [won] in every mode, the modes won at least once, the wins in a row
 * in any mode, the days played in a row, and the game that just [ended] (null when only a day was played).
 */
data class AchievementFacts(
    val played: Int,
    val won: Int,
    val modesWon: Set<GameMode>,
    val winStreak: Int,
    val dayStreak: Int,
    val ended: GameSession? = null,
)

/** When each badge is earned. */
object Achievements {
    /** The collected set of days played (ISO dates, local time): never rename. */
    const val DAYS_PLAYED = "days_played"

    private val PLAYED = mapOf(
        1 to Achievement.PLAYED_1,
        10 to Achievement.PLAYED_10,
        50 to Achievement.PLAYED_50,
        100 to Achievement.PLAYED_100,
        500 to Achievement.PLAYED_500,
    )
    private val DAYS = mapOf(
        1 to Achievement.DAYS_1,
        3 to Achievement.DAYS_3,
        5 to Achievement.DAYS_5,
        10 to Achievement.DAYS_10,
        20 to Achievement.DAYS_20,
        30 to Achievement.DAYS_30,
        50 to Achievement.DAYS_50,
        100 to Achievement.DAYS_100,
        200 to Achievement.DAYS_200,
    )
    private val WON = mapOf(
        1 to Achievement.WON_1,
        10 to Achievement.WON_10,
        50 to Achievement.WON_50,
        100 to Achievement.WON_100,
        500 to Achievement.WON_500,
        1000 to Achievement.WON_1000,
    )
    private val STREAK = mapOf(
        3 to Achievement.STREAK_3,
        5 to Achievement.STREAK_5,
        10 to Achievement.STREAK_10,
        20 to Achievement.STREAK_20,
    )
    private val MODE_WINS = mapOf(
        GameMode.STANDARD to Achievement.WIN_STANDARD,
        GameMode.VEGAS to Achievement.WIN_VEGAS,
        GameMode.VEGAS_CUMULATIVE to Achievement.WIN_VEGAS_CUMULATIVE,
        GameMode.COUNTER_TIME to Achievement.WIN_COUNTER_TIME,
    )

    private const val FAST_SECONDS = 3 * 60L
    private const val FEW_MOVES = 100

    /** Vegas pays $5 a card on a foundation against its $52 stake: 11 cards ($55) end above the starting balance. */
    private const val PROFIT_CARDS = 11

    /** [progress] with the badges [facts] earn added; what this version doesn't know is kept. */
    fun after(progress: AchievementProgress, facts: AchievementFacts): AchievementProgress =
        progress.copy(unlocked = progress.unlocked + earned(facts).map { it.name })

    fun earned(facts: AchievementFacts): Set<Achievement> = buildSet {
        addAll(reached(PLAYED, facts.played))
        addAll(reached(DAYS, facts.dayStreak))
        addAll(reached(WON, facts.won))
        addAll(reached(STREAK, facts.winStreak))
        facts.modesWon.forEach { add(MODE_WINS.getValue(it)) }
        if (facts.modesWon.containsAll(GameMode.entries)) add(Achievement.WIN_EVERY_MODE)
        facts.ended?.let { addAll(gameBadges(it)) }
    }

    /** The days in a row played up to [today], from the ISO dates in [days]. */
    fun dayStreak(days: Set<String>, today: LocalDate): Int =
        generateSequence(today) { it.minusDays(1) }.takeWhile { it.toString() in days }.count()

    private fun reached(ladder: Map<Int, Achievement>, count: Int) = ladder.filterKeys { count >= it }.values

    private fun gameBadges(session: GameSession): Set<Achievement> = buildSet {
        val state = session.state
        if (state.mode.isVegas && state.foundations.sumOf { it.size } >= PROFIT_CARDS) add(Achievement.VEGAS_PROFIT)
        if (state.mode == GameMode.VEGAS_CUMULATIVE && state.score > 0) add(Achievement.BANK_POSITIVE)
        if (!state.isWon) return@buildSet
        if (state.drawMode == DrawMode.THREE) add(Achievement.WIN_DRAW_3)
        if (state.difficulty == Difficulty.HARD) add(Achievement.WIN_HARD)
        if (session.undos == 0) add(Achievement.WIN_NO_UNDO)
        if (state.elapsedSeconds < FAST_SECONDS) add(Achievement.WIN_UNDER_3_MIN)
        if (state.moves < FEW_MOVES) add(Achievement.WIN_UNDER_100_MOVES)
    }
}
