package io.github.vinaooo.solo.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.data.game.FileSavedGameRepository
import io.github.vinaooo.solo.data.local.RoomScoreRepository
import io.github.vinaooo.solo.data.local.RoomStatsRepository
import io.github.vinaooo.solo.data.local.ScoreDao
import io.github.vinaooo.solo.data.local.SoloDatabase
import io.github.vinaooo.solo.data.local.StatsDao
import io.github.vinaooo.solo.data.settings.DataStoreSettingsRepository
import io.github.vinaooo.solo.data.system.RandomSeedSource
import io.github.vinaooo.solo.data.system.SystemClock
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.repository.ScoreRepository
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.repository.StatsRepository
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidersModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): SoloDatabase =
        Room.databaseBuilder(context, SoloDatabase::class.java, SoloDatabase.NAME).build()

    @Provides
    fun scoreDao(db: SoloDatabase): ScoreDao = db.scoreDao()

    @Provides
    fun statsDao(db: SoloDatabase): StatsDao = db.statsDao()

    @Provides
    @Singleton
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun savedGameRepository(@ApplicationContext context: Context): SavedGameRepository =
        FileSavedGameRepository(File(context.filesDir, "saved_game.json"), Dispatchers.IO)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DataBindingsModule {
    @Binds
    @Singleton
    fun scores(impl: RoomScoreRepository): ScoreRepository

    @Binds
    @Singleton
    fun stats(impl: RoomStatsRepository): StatsRepository

    @Binds
    @Singleton
    fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun seedSource(impl: RandomSeedSource): SeedSource

    @Binds
    fun clock(impl: SystemClock): Clock
}
