package io.github.vinaooo.solo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.domain.deal.DealPicker
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.repository.VegasBankRepository
import io.github.vinaooo.solo.domain.usecase.FinishGame
import io.github.vinaooo.solo.domain.usecase.LoseGame
import io.github.vinaooo.solo.domain.usecase.RecordAchievements
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.solo.domain.usecase.SaveGame
import io.github.vinaooo.solo.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Suppress("LongParameterList") // A new game reads and writes every store a finished one touches.
    fun startNewGame(
        savedGames: SavedGameRepository,
        stats: StatsRepository,
        scores: ScoreRepository,
        bank: VegasBankRepository,
        dealer: Dealer,
        deals: DealPicker,
        clock: Clock,
        achievements: RecordAchievements,
    ) = StartNewGame(savedGames, stats, scores, bank, dealer, deals, clock, achievements)

    @Provides
    fun recordAchievements(
        achievements: AchievementRepository,
        stats: StatsRepository,
        settings: SettingsRepository,
        clock: Clock,
    ) = RecordAchievements(achievements, stats, settings, clock)

    @Provides fun dealPicker(seeds: SeedSource, settings: SettingsRepository) = DealPicker(seeds, settings)

    @Provides fun resumeGame(savedGames: SavedGameRepository) = ResumeGame(savedGames)

    @Provides fun saveGame(savedGames: SavedGameRepository) = SaveGame(savedGames)

    @Provides
    @Suppress("LongParameterList") // A win touches every store, and the badges.
    fun finishGame(
        scores: ScoreRepository,
        stats: StatsRepository,
        bank: VegasBankRepository,
        savedGames: SavedGameRepository,
        clock: Clock,
        achievements: RecordAchievements,
    ) = FinishGame(scores, stats, bank, savedGames, clock, achievements)

    @Provides
    fun loseGame(stats: StatsRepository, savedGames: SavedGameRepository, achievements: RecordAchievements) =
        LoseGame(stats, savedGames, achievements)
}
