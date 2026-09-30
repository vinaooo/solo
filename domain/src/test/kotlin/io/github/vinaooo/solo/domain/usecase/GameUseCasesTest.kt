package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.withFoundation
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameUseCasesTest {

    private val savedGames = FakeSavedGameRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()

    private val wonState = emptyState()
        .copy(score = 1234, moves = 110, elapsedSeconds = 300)
        .withFoundation(0, suitRun('C'))
        .withFoundation(1, suitRun('D'))
        .withFoundation(2, suitRun('H'))
        .withFoundation(3, suitRun('S'))

    @Nested
    inner class StartNewGameTest {
        private val startNewGame = StartNewGame(savedGames, stats, Dealer(), seedSource = { 77 })

        @Test
        fun `deals a fresh game from a new seed and saves it`() = runTest {
            val session = startNewGame(DrawMode.THREE)

            session.seed shouldBe 77
            session.state shouldBe Dealer().deal(SeededShuffler(77), DrawMode.THREE)
            savedGames.saved shouldBe session
        }

        @Test
        fun `can replay a given seed`() = runTest {
            startNewGame(DrawMode.ONE, seed = 5).seed shouldBe 5
        }

        @Test
        fun `abandoning a game in progress counts as a loss`() = runTest {
            val inProgress = GameSession(1, Dealer().deal(SeededShuffler(1), DrawMode.ONE).copy(moves = 3))
            savedGames.saved = inProgress

            startNewGame(DrawMode.ONE)

            stats.stats.value shouldBe GameStats(played = 1)
        }

        @Test
        fun `replacing an untouched game is not a loss`() = runTest {
            savedGames.saved = GameSession(1, Dealer().deal(SeededShuffler(1), DrawMode.ONE))

            startNewGame(DrawMode.ONE)

            stats.stats.value shouldBe GameStats()
        }
    }

    @Nested
    inner class ResumeGameTest {
        @Test
        fun `returns the saved game in progress`() = runTest {
            val session = GameSession(3, Dealer().deal(SeededShuffler(3), DrawMode.ONE))
            savedGames.saved = session

            ResumeGame(savedGames)() shouldBe session
        }

        @Test
        fun `ignores a finished game`() = runTest {
            savedGames.saved = GameSession(3, wonState)

            ResumeGame(savedGames)().shouldBeNull()
        }
    }

    @Nested
    inner class FinishGameTest {
        private val finishGame = FinishGame(scores, stats, savedGames, clock = { 1_000L })

        @Test
        fun `records the score, counts the win and clears the saved game`() = runTest {
            savedGames.saved = GameSession(3, wonState)

            val record = finishGame(GameSession(3, wonState))

            record shouldBe ScoreRecord(1234, 300, 110, DrawMode.ONE, playedAtMillis = 1_000L)
            scores.records.value shouldContainExactly listOf(record)
            stats.stats.value shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
            savedGames.saved.shouldBeNull()
        }
    }

    @Nested
    inner class ObserveTest {
        @Test
        fun `top scores are ranked by points then fastest time, limited to ten`() = runTest {
            val records = (
                (1..12).map { ScoreRecord(it * 10, 100L - it, 50, DrawMode.ONE, 0) } +
                    ScoreRecord(120, 5, 50, DrawMode.ONE, 0) +
                    ScoreRecord(130, 99, 50, DrawMode.ONE, 0)
                ).shuffled(kotlin.random.Random(1))
            records.forEach { scores.add(it) }

            val top = ObserveTopScores(scores)().first()

            top.size shouldBe 10
            top[0] shouldBe ScoreRecord(130, 99, 50, DrawMode.ONE, 0)
            top[1] shouldBe ScoreRecord(120, 5, 50, DrawMode.ONE, 0)
            top[2] shouldBe ScoreRecord(120, 88, 50, DrawMode.ONE, 0)
            top.last().points shouldBe 50
        }

        @Test
        fun `stats are observed from the repository`() = runTest {
            stats.update { GameStats(played = 2, won = 1) }

            ObserveStats(stats)().first() shouldBe GameStats(played = 2, won = 1)
        }
    }
}
