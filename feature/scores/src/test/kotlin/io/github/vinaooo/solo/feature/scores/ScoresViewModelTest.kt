package io.github.vinaooo.solo.feature.scores

import app.cash.turbine.test
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.usecase.ObserveStats
import io.github.vinaooo.solo.domain.usecase.ObserveTopScores
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScoresViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scores = FakeScoreRepository()
    private val stats = FakeStatsRepository()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shows the top scores and the stats, updating live`() = runTest(dispatcher) {
        val vm = ScoresViewModel(ObserveTopScores(scores), ObserveStats(stats))
        val record = ScoreRecord(900, 200, 120, DrawMode.ONE, 0)

        vm.uiState.test {
            awaitItem() shouldBe ScoresUiState()
            scores.add(record)
            stats.update { GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1) }
            this@runTest.runCurrent()
            expectMostRecentItem() shouldBe ScoresUiState(
                scores = listOf(record),
                stats = GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1),
                isLoading = false,
            )
        }
    }
}
