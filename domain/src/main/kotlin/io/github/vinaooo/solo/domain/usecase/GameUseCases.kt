package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.deal.DealPicker
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.key
import io.github.vinaooo.solo.domain.model.toRecord
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.VegasBankRepository
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlinx.coroutines.flow.first

/**
 * Deals a new game in [GameMode] at a [Difficulty]; a given seed (restarting a deal) is dealt as it is. Abandoning a
 * game that was already being played counts as a loss; an abandoned Vegas game (either kind) still enters its ranking
 * with its dollars, and an abandoned cumulative Vegas game leaves the balance where it was, its $52 spent even if it
 * was never played.
 */
@Suppress("LongParameterList") // A new game reads and writes every store a finished one touches.
class StartNewGame(
    private val savedGames: SavedGameRepository,
    private val stats: StatsRepository,
    private val scores: ScoreRepository,
    private val vegasBank: VegasBankRepository,
    private val dealer: Dealer,
    private val deals: DealPicker,
    private val clock: Clock,
    private val achievements: RecordAchievements,
) {
    suspend operator fun invoke(
        drawMode: DrawMode,
        mode: GameMode = GameMode.STANDARD,
        difficulty: Difficulty = Difficulty.HARD,
        seed: Long? = null,
    ): GameSession {
        val dealt = seed ?: deals.next(drawMode, mode, difficulty)
        savedGames.load()?.let { abandon(it) }
        val bank = vegasBank.bank.first()
        val deal = dealer.deal(SeededShuffler(dealt), drawMode)
        val session = GameSession(
            dealt,
            deal.copy(mode = mode, difficulty = difficulty, score = mode.startingScore(bank)),
        )
        savedGames.save(session)
        return session
    }

    private suspend fun abandon(saved: GameSession) {
        val state = saved.state
        if (state.isWon) return
        if (state.mode == GameMode.VEGAS_CUMULATIVE) vegasBank.set(state.score)
        if (!saved.isInProgress) return
        stats.update(state.mode.key, GameStats::afterLoss)
        if (state.mode.isVegas) scores.add(state.toRecord(clock.nowMillis()))
        achievements.gameEnded(saved)
    }
}

class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()?.takeUnless { it.state.isWon }
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/**
 * Records a won game: its score in its mode's ranking (cumulative Vegas: the balance, which the win also carries
 * over), the win in the stats and the badges, and removes the saved game.
 */
@Suppress("LongParameterList") // A win touches every store, and the badges.
class FinishGame(
    private val scores: ScoreRepository,
    private val stats: StatsRepository,
    private val vegasBank: VegasBankRepository,
    private val savedGames: SavedGameRepository,
    private val clock: Clock,
    private val achievements: RecordAchievements,
) {
    suspend operator fun invoke(session: GameSession): ScoreRecord {
        val state = session.state
        val record = state.toRecord(clock.nowMillis())
        scores.add(record)
        stats.update(state.mode.key, GameStats::afterWin)
        if (state.mode == GameMode.VEGAS_CUMULATIVE) vegasBank.set(state.score)
        savedGames.clear()
        achievements.gameEnded(session)
        return record
    }
}

/** Records a game whose time ran out (counter time): a loss, and the saved game is gone, so it isn't counted again. */
class LoseGame(
    private val stats: StatsRepository,
    private val savedGames: SavedGameRepository,
    private val achievements: RecordAchievements,
) {
    suspend operator fun invoke(session: GameSession) {
        stats.update(session.state.mode.key, GameStats::afterLoss)
        savedGames.clear()
        achievements.gameEnded(session)
    }
}
