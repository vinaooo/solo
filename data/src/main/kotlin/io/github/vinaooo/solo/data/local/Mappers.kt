package io.github.vinaooo.solo.data.local

import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord

internal fun ScoreRecord.toEntity() = ScoreEntity(
    points = points,
    elapsedSeconds = elapsedSeconds,
    moves = moves,
    drawMode = drawMode.name,
    playedAtMillis = playedAtMillis,
    mode = mode.name,
)

internal fun ScoreEntity.toDomain() = ScoreRecord(
    points = points,
    elapsedSeconds = elapsedSeconds,
    moves = moves,
    drawMode = DrawMode.valueOf(drawMode),
    playedAtMillis = playedAtMillis,
    mode = gameModeOf(mode),
)

/** A mode this version doesn't know (written by a newer one) reads as Standard. */
internal fun gameModeOf(name: String): GameMode = GameMode.entries.firstOrNull { it.name == name } ?: GameMode.STANDARD

internal fun GameStats.toEntity() = StatsEntity(
    played = played,
    won = won,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    vegasBank = vegasBank,
)

internal fun StatsEntity?.toDomain() =
    this?.let { GameStats(played, won, currentStreak, bestStreak, vegasBank) } ?: GameStats()
