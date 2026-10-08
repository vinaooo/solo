package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeStatsRepository(initial: Map<String, GameStats> = emptyMap()) : StatsRepository {
    val stats = MutableStateFlow(initial)

    override fun observe(mode: String): Flow<GameStats> = stats.map { it[mode] ?: GameStats() }

    override fun observePlayedModes(): Flow<Set<String>> = stats.map { it.keys }

    override suspend fun update(mode: String, transform: (GameStats) -> GameStats) {
        stats.value += mode to transform(stats.value[mode] ?: GameStats())
    }
}
