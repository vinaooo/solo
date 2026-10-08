package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.deal.DealPicker
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.deal.WinnableDeals
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.fake.FakeVegasBankRepository
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.difficulty
import io.github.vinaooo.solo.domain.model.drawMode
import io.github.vinaooo.solo.domain.model.gameMode
import io.github.vinaooo.solo.domain.model.moves
import io.github.vinaooo.solo.domain.model.ranking
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameUseCasesTest {

    private val savedGames = FakeSavedGameRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val settings = FakeSettingsRepository()
    private val bank = FakeVegasBankRepository()

    private val wonState = emptyState()
        .copy(score = 1234, moves = 110, elapsedSeconds = 300)
        .withFoundation(0, suitRun('C'))
        .withFoundation(1, suitRun('D'))
        .withFoundation(2, suitRun('H'))
        .withFoundation(3, suitRun('S'))

    @Nested
    inner class StartNewGameTest {
        private val startNewGame =
            StartNewGame(savedGames, stats, scores, bank, Dealer(), DealPicker({ 77 }, settings), clock = { 9L })

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
        fun `easy and normal deal a winnable seed, and the game keeps its difficulty`() = runTest {
            val easy = startNewGame(DrawMode.ONE, GameMode.VEGAS, Difficulty.EASY)

            easy.seed shouldBe WinnableDeals.seedAt(77, DrawMode.ONE, GameMode.VEGAS, Difficulty.EASY)
            easy.state.difficulty shouldBe Difficulty.EASY
            startNewGame(DrawMode.THREE, difficulty = Difficulty.NORMAL).seed shouldBe
                WinnableDeals.seedAt(78, DrawMode.THREE, GameMode.STANDARD, Difficulty.NORMAL)
        }

        @Test
        fun `a replayed seed is dealt as it is, whatever the difficulty`() = runTest {
            startNewGame(DrawMode.ONE, difficulty = Difficulty.EASY, seed = 5).seed shouldBe 5
        }

        @Test
        fun `an abandoned game's score keeps its difficulty`() = runTest {
            val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
            savedGames.saved = GameSession(
                1,
                deal.copy(mode = GameMode.VEGAS, difficulty = Difficulty.EASY, score = -37, moves = 20),
            )

            startNewGame(DrawMode.ONE, GameMode.VEGAS)

            scores.records.value.single().difficulty shouldBe Difficulty.EASY
        }

        @Test
        fun `abandoning a game in progress counts as a loss`() = runTest {
            val inProgress = GameSession(1, Dealer().deal(SeededShuffler(1), DrawMode.ONE).copy(moves = 3))
            savedGames.saved = inProgress

            startNewGame(DrawMode.ONE)

            stats.stats.value shouldBe mapOf("STANDARD" to GameStats(played = 1))
        }

        @Test
        fun `replacing an untouched game is not a loss`() = runTest {
            savedGames.saved = GameSession(1, Dealer().deal(SeededShuffler(1), DrawMode.ONE))

            startNewGame(DrawMode.ONE)

            stats.stats.value shouldBe emptyMap()
        }

        @Test
        fun `a game is dealt in its mode, Vegas paying its $52 up front`() = runTest {
            startNewGame(DrawMode.ONE, GameMode.VEGAS).state.run {
                mode shouldBe GameMode.VEGAS
                score shouldBe -52
            }
            startNewGame(DrawMode.ONE, GameMode.COUNTER_TIME).state.score shouldBe 0
        }

        @Test
        fun `cumulative Vegas starts from the carried balance`() = runTest {
            bank.set(30)

            startNewGame(DrawMode.ONE, GameMode.VEGAS_CUMULATIVE).state.score shouldBe -22
        }

        @Test
        fun `an abandoned Vegas game enters the Vegas ranking with its dollars`() = runTest {
            val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
            savedGames.saved = GameSession(1, deal.copy(mode = GameMode.VEGAS, score = -37, moves = 20))

            startNewGame(DrawMode.ONE, GameMode.VEGAS)

            scores.records.value shouldContainExactly listOf(
                ScoreRecord(
                    "VEGAS",
                    -37,
                    0,
                    9L,
                    mapOf("moves" to "20", "drawMode" to "ONE", "difficulty" to "HARD"),
                ),
            )
            stats.stats.value.getValue("VEGAS").played shouldBe 1
        }

        @Test
        fun `an abandoned cumulative game leaves the balance where it was, even untouched`() = runTest {
            val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
            savedGames.saved = GameSession(1, deal.copy(mode = GameMode.VEGAS_CUMULATIVE, score = -52))

            val next = startNewGame(DrawMode.ONE, GameMode.VEGAS_CUMULATIVE)

            bank.bank.value shouldBe -52
            next.state.score shouldBe -104
            scores.records.value.shouldBeEmpty()
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
        private val finishGame = FinishGame(scores, stats, bank, savedGames, clock = { 1_000L })

        @Test
        fun `records the score, counts the win and clears the saved game`() = runTest {
            savedGames.saved = GameSession(3, wonState)

            val record = finishGame(GameSession(3, wonState))

            record.points shouldBe 1234
            record.elapsedSeconds shouldBe 300
            record.moves shouldBe 110
            record.drawMode shouldBe DrawMode.ONE
            record.gameMode shouldBe GameMode.STANDARD
            record.playedAtMillis shouldBe 1_000L
            scores.records.value shouldContainExactly listOf(record)
            stats.stats.value shouldBe
                mapOf("STANDARD" to GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1))
            savedGames.saved.shouldBeNull()
        }

        @Test
        fun `a cumulative win carries its balance over and enters no ranking`() = runTest {
            val won = wonState.copy(mode = GameMode.VEGAS_CUMULATIVE, score = 156)

            finishGame(GameSession(3, won))

            bank.bank.value shouldBe 156
            stats.stats.value.getValue("VEGAS_CUMULATIVE").won shouldBe 1
            scores.records.value.shouldBeEmpty()
        }
    }

    @Nested
    inner class LoseGameTest {
        @Test
        fun `a game out of time is a loss, and gone, so a new game doesn't count it again`() = runTest {
            val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
            savedGames.saved = GameSession(1, deal.copy(mode = GameMode.COUNTER_TIME, moves = 5, elapsedSeconds = 600))

            LoseGame(stats, savedGames)(GameMode.COUNTER_TIME)
            StartNewGame(savedGames, stats, scores, bank, Dealer(), DealPicker({ 2 }, settings), clock = { 0L })(
                DrawMode.ONE,
            )

            stats.stats.value shouldBe mapOf("COUNTER_TIME" to GameStats(played = 1))
        }
    }

    @Test
    fun `each mode ranks its own way, and cumulative Vegas not at all`() {
        GameMode.STANDARD.ranking() shouldBe Ranking.HIGHEST_POINTS
        GameMode.VEGAS.ranking() shouldBe Ranking.HIGHEST_POINTS
        GameMode.COUNTER_TIME.ranking() shouldBe Ranking.FASTEST
        GameMode.VEGAS_CUMULATIVE.ranking().shouldBeNull()
    }
}
