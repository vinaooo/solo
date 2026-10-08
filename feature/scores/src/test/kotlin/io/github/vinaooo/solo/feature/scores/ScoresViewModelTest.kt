package io.github.vinaooo.solo.feature.scores

import app.cash.turbine.test
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.Ranking
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScoresViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scores = FakeScoreRepository(
        listOf(
            soloRecord(900, 300, 100, DrawMode.ONE, 0, GameMode.COUNTER_TIME),
            soloRecord(100, 200, 100, DrawMode.ONE, 0, GameMode.COUNTER_TIME),
        ),
    )
    private val stats = FakeStatsRepository(
        mapOf(
            "VEGAS_CUMULATIVE" to GameStats(played = 2),
            "COUNTER_TIME" to GameStats(played = 2, won = 2),
        ),
    )

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a tab per mode played, in Solo's order, each in its own ranking`() = runTest(dispatcher) {
        val vm = SoloScoresViewModel(scores, stats)
        vm.uiState.test {
            skipItems(1)
            val state = awaitItem()
            state.groups shouldBe listOf("VEGAS_CUMULATIVE", "COUNTER_TIME")
            state.sections.single().ranking shouldBe null

            vm.selectGroup("COUNTER_TIME")
            val timed = awaitItem().sections.single()
            timed.ranking shouldBe Ranking.FASTEST
            timed.scores.map { it.elapsedSeconds } shouldBe listOf(200L, 300L)
        }
    }
}
