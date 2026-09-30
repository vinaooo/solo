package io.github.vinaooo.solo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.solo.domain.model.GameStats
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomStatsRepositoryTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SoloDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = RoomStatsRepository(db, db.statsDao())

    @After
    fun tearDown() = db.close()

    @Test
    fun `no stats yet reads as zeros`() = runTest {
        repository.observe().first() shouldBe GameStats()
    }

    @Test
    fun `updates are applied on top of the stored stats`() = runTest {
        repository.update { it.afterWin() }
        repository.update { it.afterWin() }
        repository.update { it.afterLoss() }

        repository.observe().first() shouldBe GameStats(played = 3, won = 2, currentStreak = 0, bestStreak = 2)
    }
}
