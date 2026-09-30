package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStatsRepository(initial: GameStats = GameStats()) : StatsRepository {
    val stats = MutableStateFlow(initial)

    override fun observe(): Flow<GameStats> = stats

    override suspend fun update(transform: (GameStats) -> GameStats) {
        stats.value = transform(stats.value)
    }
}
