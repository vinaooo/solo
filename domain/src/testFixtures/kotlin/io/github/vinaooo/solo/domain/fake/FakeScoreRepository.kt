package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScoreRepository(initial: List<ScoreRecord> = emptyList()) : ScoreRepository {
    val records = MutableStateFlow(initial)

    override fun observeTopScores(mode: String, ranking: Ranking, limit: Int): Flow<List<ScoreRecord>> =
        records.map { all -> all.filter { it.mode == mode }.sortedWith(ranking.comparator).take(limit) }

    override suspend fun add(record: ScoreRecord) {
        records.value += record
    }
}
