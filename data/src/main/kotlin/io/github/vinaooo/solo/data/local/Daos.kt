package io.github.vinaooo.solo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: ScoreEntity)

    @Query("SELECT * FROM scores ORDER BY points DESC, elapsedSeconds ASC LIMIT :limit")
    fun observeTop(limit: Int): Flow<List<ScoreEntity>>
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.SINGLE_ROW_ID}")
    fun observe(): Flow<StatsEntity?>

    @Query("SELECT * FROM stats WHERE id = ${StatsEntity.SINGLE_ROW_ID}")
    suspend fun get(): StatsEntity?

    @Upsert
    suspend fun upsert(stats: StatsEntity)
}
