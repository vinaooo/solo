package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord

/** Solo's scores are vinkit's: the mode's key is [GameMode]'s name, and the game's details go in the extras. */
val GameMode.key: String get() = name

/** How each mode ranks: counter time by the fastest win, cumulative Vegas not at all (by the user's choice). */
fun GameMode.ranking(): Ranking? = when (this) {
    GameMode.COUNTER_TIME -> Ranking.FASTEST
    GameMode.VEGAS_CUMULATIVE -> null
    else -> Ranking.HIGHEST_POINTS
}

/** This game as a score played at [nowMillis]. */
fun GameState.toRecord(nowMillis: Long) = ScoreRecord(
    mode = mode.key,
    points = score,
    elapsedSeconds = elapsedSeconds,
    playedAtMillis = nowMillis,
    extras = mapOf(MOVES to moves.toString(), DRAW_MODE to drawMode.name, DIFFICULTY to difficulty.name),
)

val ScoreRecord.gameMode: GameMode get() = enumValueOf(mode)
val ScoreRecord.moves: Int get() = extras[MOVES]?.toIntOrNull() ?: 0
val ScoreRecord.drawMode: DrawMode get() = enumOrNull<DrawMode>(extras[DRAW_MODE]) ?: DrawMode.ONE
val ScoreRecord.difficulty: Difficulty get() = enumOrNull<Difficulty>(extras[DIFFICULTY]) ?: Difficulty.HARD

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? = enumValues<T>().firstOrNull { it.name == name }

private const val MOVES = "moves"
private const val DRAW_MODE = "drawMode"
private const val DIFFICULTY = "difficulty"
