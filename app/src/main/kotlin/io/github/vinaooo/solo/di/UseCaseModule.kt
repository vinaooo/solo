package io.github.vinaooo.solo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.domain.deal.DealPicker
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.repository.StatsRepository
import io.github.vinaooo.solo.domain.usecase.FinishGame
import io.github.vinaooo.solo.domain.usecase.LoseGame
import io.github.vinaooo.solo.domain.usecase.ObserveRankedModes
import io.github.vinaooo.solo.domain.usecase.ObserveStats
import io.github.vinaooo.solo.domain.usecase.ObserveTopScores
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.solo.domain.usecase.SaveGame
import io.github.vinaooo.solo.domain.usecase.StartNewGame

/** Domain use cases, wired to the data layer's repositories. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Suppress("LongParameterList") // A new game reads and writes every store a finished one touches.
    fun startNewGame(
        savedGames: SavedGameRepository,
        stats: StatsRepository,
        scores: ScoreRepository,
        dealer: Dealer,
        deals: DealPicker,
        clock: Clock,
    ) = StartNewGame(savedGames, stats, scores, dealer, deals, clock)

    @Provides fun dealPicker(seeds: SeedSource, settings: SettingsRepository) = DealPicker(seeds, settings)

    @Provides fun resumeGame(savedGames: SavedGameRepository) = ResumeGame(savedGames)

    @Provides fun saveGame(savedGames: SavedGameRepository) = SaveGame(savedGames)

    @Provides
    fun finishGame(scores: ScoreRepository, stats: StatsRepository, savedGames: SavedGameRepository, clock: Clock) =
        FinishGame(scores, stats, savedGames, clock)

    @Provides fun loseGame(stats: StatsRepository, savedGames: SavedGameRepository) = LoseGame(stats, savedGames)

    @Provides fun observeTopScores(scores: ScoreRepository) = ObserveTopScores(scores)

    @Provides fun observeRankedModes(scores: ScoreRepository) = ObserveRankedModes(scores)

    @Provides fun observeStats(stats: StatsRepository) = ObserveStats(stats)
}
