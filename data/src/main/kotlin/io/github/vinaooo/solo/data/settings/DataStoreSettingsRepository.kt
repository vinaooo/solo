package io.github.vinaooo.solo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Solo's own settings, in the DataStore vinkit's `DataStoreAppSettingsRepository` keeps the common ones in: each writes
 * only its own keys.
 */
class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            drawMode = enumOrDefault(this[Keys.DRAW_MODE], defaults.drawMode),
            gameMode = enumOrDefault(this[Keys.GAME_MODE], defaults.gameMode),
            difficulty = enumOrDefault(this[Keys.DIFFICULTY], defaults.difficulty),
            autoCompleteTipsShown = this[Keys.AUTO_COMPLETE_TIPS_SHOWN] ?: defaults.autoCompleteTipsShown,
            dealCursor = this[Keys.DEAL_CURSOR],
            winStreak = this[Keys.WIN_STREAK] ?: defaults.winStreak,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.DRAW_MODE] = settings.drawMode.name
        this[Keys.GAME_MODE] = settings.gameMode.name
        this[Keys.DIFFICULTY] = settings.difficulty.name
        this[Keys.AUTO_COMPLETE_TIPS_SHOWN] = settings.autoCompleteTipsShown
        this[Keys.WIN_STREAK] = settings.winStreak
        settings.dealCursor?.let { this[Keys.DEAL_CURSOR] = it } ?: remove(Keys.DEAL_CURSOR)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val DRAW_MODE = stringPreferencesKey("draw_mode")
        val GAME_MODE = stringPreferencesKey("game_mode")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val AUTO_COMPLETE_TIPS_SHOWN = intPreferencesKey("auto_complete_tips_shown")
        val DEAL_CURSOR = longPreferencesKey("deal_cursor")
        val WIN_STREAK = intPreferencesKey("win_streak")
    }
}
