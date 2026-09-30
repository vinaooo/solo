package io.github.vinaooo.solo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val points: Int,
    val elapsedSeconds: Long,
    val moves: Int,
    val drawMode: String,
    val playedAtMillis: Long,
)

/** Single-row table: the lifetime statistics. */
@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val played: Int,
    val won: Int,
    val currentStreak: Int,
    val bestStreak: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
