package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.StatsRepository
import io.github.vinaooo.solo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Deals a new game in [GameMode]. Abandoning a game that was already being played counts as a loss; an abandoned
 * Vegas game still enters the Vegas ranking with the dollars it made, and an abandoned cumulative Vegas game leaves
 * the balance where it was, its $52 spent even if it was never played.
 */
class StartNewGame(
    private val savedGames: SavedGameRepository,
    private val stats: StatsRepository,
    private val scores: ScoreRepository,
    private val dealer: Dealer,
    private val seedSource: SeedSource,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        drawMode: DrawMode,
        mode: GameMode = GameMode.STANDARD,
        seed: Long = seedSource.nextSeed(),
    ): GameSession {
        savedGames.load()?.let { abandon(it) }
        val bank = stats.observe().first().vegasBank
        val deal = dealer.deal(SeededShuffler(seed), drawMode)
        val session = GameSession(seed, deal.copy(mode = mode, score = mode.startingScore(bank)))
        savedGames.save(session)
        return session
    }

    private suspend fun abandon(saved: GameSession) {
        val state = saved.state
        if (state.isWon) return
        if (state.mode == GameMode.VEGAS_CUMULATIVE) stats.update { it.copy(vegasBank = state.score) }
        if (!saved.isInProgress) return
        stats.update(GameStats::afterLoss)
        if (state.mode == GameMode.VEGAS) scores.add(state.toRecord(clock.nowMillis()))
    }
}

class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()?.takeUnless { it.state.isWon }
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/**
 * Records a won game: its score in its mode's ranking (cumulative Vegas has none, only its balance, which the win
 * carries over), the win in the stats, and removes the saved game.
 */
class FinishGame(
    private val scores: ScoreRepository,
    private val stats: StatsRepository,
    private val savedGames: SavedGameRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(session: GameSession): ScoreRecord {
        val state = session.state
        val record = state.toRecord(clock.nowMillis())
        if (ScoreRecord.isRanked(state.mode)) scores.add(record)
        stats.update { won ->
            won.afterWin().let { if (state.mode == GameMode.VEGAS_CUMULATIVE) it.copy(vegasBank = state.score) else it }
        }
        savedGames.clear()
        return record
    }
}

/** Records a game whose time ran out (counter time): a loss, and the saved game is gone, so it isn't counted again. */
class LoseGame(private val stats: StatsRepository, private val savedGames: SavedGameRepository) {
    suspend operator fun invoke() {
        stats.update(GameStats::afterLoss)
        savedGames.clear()
    }
}

class ObserveTopScores(private val scores: ScoreRepository) {
    operator fun invoke(mode: GameMode): Flow<List<ScoreRecord>> = scores.observeTopScores(mode, ScoreRecord.TOP_LIMIT)
}

/** The modes already played to a ranked score, for the Scores screen's tabs. */
class ObserveRankedModes(private val scores: ScoreRepository) {
    operator fun invoke(): Flow<Set<GameMode>> = scores.observeRankedModes()
}

class ObserveStats(private val stats: StatsRepository) {
    operator fun invoke(): Flow<GameStats> = stats.observe()
}

private fun GameState.toRecord(nowMillis: Long) = ScoreRecord(score, elapsedSeconds, moves, drawMode, nowMillis, mode)
