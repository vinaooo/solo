package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScoreRepository(initial: List<ScoreRecord> = emptyList()) : ScoreRepository {
    val records = MutableStateFlow(initial)

    override fun observeTopScores(limit: Int): Flow<List<ScoreRecord>> =
        records.map { all -> all.sortedWith(ScoreRecord.RANKING).take(limit) }

    override suspend fun add(record: ScoreRecord) {
        records.value += record
    }
}
