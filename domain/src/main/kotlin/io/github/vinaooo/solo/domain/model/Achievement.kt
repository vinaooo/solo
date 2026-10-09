package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.vinkit.core.AchievementProgress
import java.time.LocalDate

/**
 * A badge. Its name is its key in storage (vinkit's `AchievementProgress`): never rename one after release.
 * A badge on a [ladder] is earned once that count reaches [count].
 */
enum class Achievement(val ladder: Ladder? = null, val count: Int = 0) {
    PLAYED_1(Ladder.PLAYED, 1),
    PLAYED_10(Ladder.PLAYED, 10),
    PLAYED_50(Ladder.PLAYED, 50),
    PLAYED_100(Ladder.PLAYED, 100),
    PLAYED_500(Ladder.PLAYED, 500),
    DAYS_1(Ladder.DAYS, 1),
    DAYS_3(Ladder.DAYS, 3),
    DAYS_5(Ladder.DAYS, 5),
    DAYS_10(Ladder.DAYS, 10),
    DAYS_20(Ladder.DAYS, 20),
    DAYS_30(Ladder.DAYS, 30),
    DAYS_50(Ladder.DAYS, 50),
    DAYS_100(Ladder.DAYS, 100),
    DAYS_200(Ladder.DAYS, 200),
    WON_1(Ladder.WON, 1),
    WON_10(Ladder.WON, 10),
    WON_50(Ladder.WON, 50),
    WON_100(Ladder.WON, 100),
    WON_500(Ladder.WON, 500),
    WON_1000(Ladder.WON, 1000),
    STREAK_3(Ladder.STREAK, 3),
    STREAK_5(Ladder.STREAK, 5),
    STREAK_10(Ladder.STREAK, 10),
    STREAK_20(Ladder.STREAK, 20),
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

/** What a ladder of badges counts. */
enum class Ladder {
    /** Games played, in every mode. */
    PLAYED,

    /** Days played in a row. */
    DAYS,

    /** Games won, in every mode. */
    WON,

    /** Games won in a row, in any mode. */
    STREAK,
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
        val counts = mapOf(
            Ladder.PLAYED to facts.played,
            Ladder.DAYS to facts.dayStreak,
            Ladder.WON to facts.won,
            Ladder.STREAK to facts.winStreak,
        )
        addAll(Achievement.entries.filter { badge -> badge.ladder?.let { counts.getValue(it) >= badge.count } == true })
        facts.modesWon.forEach { add(MODE_WINS.getValue(it)) }
        if (facts.modesWon.containsAll(GameMode.entries)) add(Achievement.WIN_EVERY_MODE)
        facts.ended?.let { addAll(gameBadges(it)) }
    }

    /** The days in a row played up to [today], from the ISO dates in [days]. */
    fun dayStreak(days: Set<String>, today: LocalDate): Int =
        generateSequence(today) { it.minusDays(1) }.takeWhile { it.toString() in days }.count()

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
