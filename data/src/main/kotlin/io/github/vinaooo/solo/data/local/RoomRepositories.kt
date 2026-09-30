package io.github.vinaooo.solo.data.local

import androidx.room.withTransaction
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import io.github.vinaooo.solo.domain.repository.StatsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScoreRepository @Inject constructor(private val dao: ScoreDao) : ScoreRepository {
    override fun observeTopScores(limit: Int): Flow<List<ScoreRecord>> =
        dao.observeTop(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(record: ScoreRecord) = dao.insert(record.toEntity())
}

class RoomStatsRepository @Inject constructor(private val db: SoloDatabase, private val dao: StatsDao) :
    StatsRepository {
    override fun observe(): Flow<GameStats> = dao.observe().map { it.toDomain() }

    override suspend fun update(transform: (GameStats) -> GameStats) = db.withTransaction {
        dao.upsert(transform(dao.get().toDomain()).toEntity())
    }
}
