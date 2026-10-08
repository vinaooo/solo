package io.github.vinaooo.solo.domain.repository

import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun load(): GameSession?

    suspend fun save(session: GameSession)

    suspend fun clear()
}

/** Cumulative Vegas's balance in dollars, carried from one game to the next. */
interface VegasBankRepository {
    val bank: Flow<Int>

    suspend fun set(dollars: Int)
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
