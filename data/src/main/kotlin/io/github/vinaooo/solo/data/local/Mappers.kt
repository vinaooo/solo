package io.github.vinaooo.solo.data.local

import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord

internal fun ScoreRecord.toEntity() = ScoreEntity(
    points = points,
    elapsedSeconds = elapsedSeconds,
    moves = moves,
    drawMode = drawMode.name,
    playedAtMillis = playedAtMillis,
)

internal fun ScoreEntity.toDomain() = ScoreRecord(
    points = points,
    elapsedSeconds = elapsedSeconds,
    moves = moves,
    drawMode = DrawMode.valueOf(drawMode),
    playedAtMillis = playedAtMillis,
)

internal fun GameStats.toEntity() = StatsEntity(
    played = played,
    won = won,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
)

internal fun StatsEntity?.toDomain() = this?.let { GameStats(played, won, currentStreak, bestStreak) } ?: GameStats()
