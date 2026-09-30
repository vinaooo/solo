package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.StatsRepository
import io.github.vinaooo.solo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow

/** Deals a new game. Abandoning a game that was already being played counts as a loss. */
class StartNewGame(
    private val savedGames: SavedGameRepository,
    private val stats: StatsRepository,
    private val dealer: Dealer,
    private val seedSource: SeedSource,
) {
    suspend operator fun invoke(drawMode: DrawMode, seed: Long = seedSource.nextSeed()): GameSession {
        if (savedGames.load()?.isInProgress == true) stats.update(GameStats::afterLoss)
        val session = GameSession(seed, dealer.deal(SeededShuffler(seed), drawMode))
        savedGames.save(session)
        return session
    }
}

class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()?.takeUnless { it.state.isWon }
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/** Records a won game: its score, the win in the stats, and removes the saved game. */
class FinishGame(
    private val scores: ScoreRepository,
    private val stats: StatsRepository,
    private val savedGames: SavedGameRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(session: GameSession): ScoreRecord {
        val state = session.state
        val record = ScoreRecord(state.score, state.elapsedSeconds, state.moves, state.drawMode, clock.nowMillis())
        scores.add(record)
        stats.update(GameStats::afterWin)
        savedGames.clear()
        return record
    }
}

class ObserveTopScores(private val scores: ScoreRepository) {
    operator fun invoke(): Flow<List<ScoreRecord>> = scores.observeTopScores(ScoreRecord.TOP_LIMIT)
}

class ObserveStats(private val stats: StatsRepository) {
    operator fun invoke(): Flow<GameStats> = stats.observe()
}
