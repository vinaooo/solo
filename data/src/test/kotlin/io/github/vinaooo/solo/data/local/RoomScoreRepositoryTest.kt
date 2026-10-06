package io.github.vinaooo.solo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
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
        repository.observeTopScores(GameMode.STANDARD).first().shouldBeEmpty()
        repository.observeRankedModes().first().shouldBeEmpty()
    }

    @Test
    fun `stores scores and returns the best first, ties broken by time, limited`() = runTest {
        val slow = ScoreRecord(500, 300, 120, DrawMode.ONE, playedAtMillis = 1)
        val fast = ScoreRecord(500, 200, 110, DrawMode.THREE, playedAtMillis = 2)
        val best = ScoreRecord(900, 400, 150, DrawMode.ONE, playedAtMillis = 3)
        val worst = ScoreRecord(100, 100, 90, DrawMode.ONE, playedAtMillis = 4)
        listOf(slow, worst, fast, best).forEach { repository.add(it) }

        repository.observeTopScores(GameMode.STANDARD, limit = 3).first() shouldContainExactly listOf(best, fast, slow)
    }

    @Test
    fun `each mode keeps its own ranking, counter time the fastest first`() = runTest {
        val vegas = ScoreRecord(-12, 300, 90, DrawMode.ONE, playedAtMillis = 1, mode = GameMode.VEGAS)
        val richer = ScoreRecord(40, 500, 120, DrawMode.ONE, playedAtMillis = 2, mode = GameMode.VEGAS)
        val slow = ScoreRecord(700, 400, 100, DrawMode.ONE, playedAtMillis = 3, mode = GameMode.COUNTER_TIME)
        val fast = ScoreRecord(500, 250, 130, DrawMode.ONE, playedAtMillis = 4, mode = GameMode.COUNTER_TIME)
        val standard = ScoreRecord(900, 200, 120, DrawMode.ONE, playedAtMillis = 5)
        listOf(vegas, richer, slow, fast, standard).forEach { repository.add(it) }

        repository.observeTopScores(GameMode.VEGAS).first() shouldContainExactly listOf(richer, vegas)
        repository.observeTopScores(GameMode.COUNTER_TIME).first() shouldContainExactly listOf(fast, slow)
        repository.observeTopScores(GameMode.STANDARD).first() shouldContainExactly listOf(standard)
        repository.observeRankedModes().first() shouldBe
            setOf(GameMode.STANDARD, GameMode.VEGAS, GameMode.COUNTER_TIME)
    }

    @Test
    fun `a score keeps its difficulty`() = runTest {
        val easy = ScoreRecord(300, 200, 100, DrawMode.ONE, playedAtMillis = 1, difficulty = Difficulty.EASY)

        repository.add(easy)

        repository.observeTopScores(GameMode.STANDARD).first() shouldContainExactly listOf(easy)
    }
}
