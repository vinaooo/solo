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

    /** A mode's best scores: the most points first, or the fastest first when [fastest] (counter time). */
    @Query(
        "SELECT * FROM scores WHERE mode = :mode ORDER BY " +
            "CASE WHEN :fastest THEN elapsedSeconds END ASC, CASE WHEN :fastest THEN moves END ASC, " +
            "points DESC, elapsedSeconds ASC LIMIT :limit",
    )
    fun observeTop(mode: String, fastest: Boolean, limit: Int): Flow<List<ScoreEntity>>

    @Query("SELECT DISTINCT mode FROM scores")
    fun observeModes(): Flow<List<String>>
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
