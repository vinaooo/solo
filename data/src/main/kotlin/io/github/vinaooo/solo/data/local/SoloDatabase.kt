package io.github.vinaooo.solo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScoreEntity::class, StatsEntity::class], version = 1, exportSchema = true)
abstract class SoloDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    abstract fun statsDao(): StatsDao

    companion object {
        const val NAME = "solo.db"
    }
}
