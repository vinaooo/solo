package io.github.vinaooo.solo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomScoreRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SoloDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = RoomScoreRepository(db.scoreDao())

    @After
    fun tearDown() = db.close()

    @Test
    fun `starts empty`() = runTest {
        repository.observeTopScores().first().shouldBeEmpty()
    }

    @Test
    fun `stores scores and returns the best first, ties broken by time, limited`() = runTest {
        val slow = ScoreRecord(500, 300, 120, DrawMode.ONE, playedAtMillis = 1)
        val fast = ScoreRecord(500, 200, 110, DrawMode.THREE, playedAtMillis = 2)
        val best = ScoreRecord(900, 400, 150, DrawMode.ONE, playedAtMillis = 3)
        val worst = ScoreRecord(100, 100, 90, DrawMode.ONE, playedAtMillis = 4)
        listOf(slow, worst, fast, best).forEach { repository.add(it) }

        repository.observeTopScores(limit = 3).first() shouldContainExactly listOf(best, fast, slow)
    }
}
