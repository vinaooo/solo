package io.github.vinaooo.solo.feature.scores

import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.vinkit.core.ScoreRecord

/** A score as Solo records it. */
@Suppress("LongParameterList") // Every field a score shows.
internal fun soloRecord(
    points: Int,
    elapsedSeconds: Long,
    moves: Int,
    drawMode: DrawMode,
    playedAtMillis: Long,
    mode: GameMode = GameMode.STANDARD,
    difficulty: Difficulty = Difficulty.HARD,
) = ScoreRecord(
    mode.name,
    points,
    elapsedSeconds,
    playedAtMillis,
    mapOf("moves" to "$moves", "drawMode" to drawMode.name, "difficulty" to difficulty.name),
)
