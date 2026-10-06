package io.github.vinaooo.solo.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Version 2 added game modes: each score's mode and the cumulative Vegas balance. Version 3 added each score's
 * difficulty.
 */
@Database(
    entities = [ScoreEntity::class, StatsEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class SoloDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "solo.db"
    }
}
