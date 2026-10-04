package io.github.vinaooo.solo.feature.scores

import app.cash.turbine.test
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.usecase.ObserveRankedModes
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
        val vm = ScoresViewModel(ObserveTopScores(scores), ObserveStats(stats), ObserveRankedModes(scores))
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
                modes = listOf(GameMode.STANDARD),
            )
        }
    }

    @Test
    fun `a tab per mode played, the first shown until another is chosen`() = runTest(dispatcher) {
        val vm = ScoresViewModel(ObserveTopScores(scores), ObserveStats(stats), ObserveRankedModes(scores))
        val vegas = ScoreRecord(-7, 300, 80, DrawMode.ONE, 0, GameMode.VEGAS)
        val timed = ScoreRecord(0, 240, 110, DrawMode.ONE, 0, GameMode.COUNTER_TIME)
        scores.add(timed)
        scores.add(vegas)

        vm.uiState.test {
            this@runTest.runCurrent()
            val first = expectMostRecentItem()
            first.modes shouldBe listOf(GameMode.VEGAS, GameMode.COUNTER_TIME)
            first.mode shouldBe GameMode.VEGAS
            first.scores shouldBe listOf(vegas)
            vm.selectMode(GameMode.COUNTER_TIME)
            this@runTest.runCurrent()
            expectMostRecentItem().scores shouldBe listOf(timed)
        }
    }
}
