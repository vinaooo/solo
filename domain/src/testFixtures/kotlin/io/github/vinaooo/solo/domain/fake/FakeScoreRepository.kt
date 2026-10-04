package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeScoreRepository(initial: List<ScoreRecord> = emptyList()) : ScoreRepository {
    val records = MutableStateFlow(initial)

    override fun observeTopScores(mode: GameMode, limit: Int): Flow<List<ScoreRecord>> =
        records.map { all -> all.filter { it.mode == mode }.sortedWith(ScoreRecord.rankingFor(mode)).take(limit) }

    override fun observeRankedModes(): Flow<Set<GameMode>> =
        records.map { all -> all.map { it.mode }.filter(ScoreRecord::isRanked).toSet() }

    override suspend fun add(record: ScoreRecord) {
        records.value += record
    }
}
