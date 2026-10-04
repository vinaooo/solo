package io.github.vinaooo.solo.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The scores and stats a player already has survive the move to game modes (version 1 to 2): a version 1 database,
 * built from its exported schema, is opened by the current app, which runs the real migration.
 */
@RunWith(RobolectricTestRunner::class)
class SoloDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `version 1 scores become Standard scores, and the stats keep their values with no balance`() = runTest {
        val file = context.getDatabasePath(NAME).also { it.parentFile?.mkdirs() }
        createVersion1(file)

        val migrated = Room.databaseBuilder(context, SoloDatabase::class.java, NAME).allowMainThreadQueries().build()
        try {
            val standard = RoomScoreRepository(migrated.scoreDao()).observeTopScores(GameMode.STANDARD).first()
            standard shouldHaveSize 1
            standard.single().points shouldBe 900
            RoomStatsRepository(migrated, migrated.statsDao()).observe().first() shouldBe
                GameStats(played = 5, won = 3, currentStreak = 1, bestStreak = 2, vegasBank = 0)
        } finally {
            migrated.close()
        }
    }

    /** The tables and identity of schema 1.json, with a score and the stats row in them. */
    private fun createVersion1(file: File) {
        val schema = Json.parseToJsonElement(File(SCHEMA_1).readText()).jsonObject.getValue("database").jsonObject
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
                db.execSQL(
                    entity.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table),
                )
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL(
                "INSERT INTO scores (points, elapsedSeconds, moves, drawMode, playedAtMillis) " +
                    "VALUES (900, 200, 120, 'ONE', 7)",
            )
            db.execSQL("INSERT INTO stats (id, played, won, currentStreak, bestStreak) VALUES (0, 5, 3, 1, 2)")
            db.version = 1
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
        const val SCHEMA_1 = "schemas/io.github.vinaooo.solo.data.local.SoloDatabase/1.json"
    }
}
