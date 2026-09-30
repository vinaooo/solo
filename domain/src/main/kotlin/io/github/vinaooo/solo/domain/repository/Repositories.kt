package io.github.vinaooo.solo.domain.repository

import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun load(): GameSession?

    suspend fun save(session: GameSession)

    suspend fun clear()
}

interface ScoreRepository {
    fun observeTopScores(limit: Int = ScoreRecord.TOP_LIMIT): Flow<List<ScoreRecord>>

    suspend fun add(record: ScoreRecord)
}

interface StatsRepository {
    fun observe(): Flow<GameStats>

    suspend fun update(transform: (GameStats) -> GameStats)
}

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun update(transform: (Settings) -> Settings)
}

fun interface SeedSource {
    fun nextSeed(): Long
}

fun interface Clock {
    fun nowMillis(): Long
}
