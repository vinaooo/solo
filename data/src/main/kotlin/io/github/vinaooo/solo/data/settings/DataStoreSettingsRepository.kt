package io.github.vinaooo.solo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            soundEnabled = this[Keys.SOUND] ?: defaults.soundEnabled,
            hapticsEnabled = this[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            showTimer = this[Keys.SHOW_TIMER] ?: defaults.showTimer,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.DRAW_MODE] = settings.drawMode.name
        this[Keys.THEME_MODE] = settings.themeMode.name
        this[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        this[Keys.SOUND] = settings.soundEnabled
        this[Keys.HAPTICS] = settings.hapticsEnabled
        this[Keys.SHOW_TIMER] = settings.showTimer
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val DRAW_MODE = stringPreferencesKey("draw_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SHOW_TIMER = booleanPreferencesKey("show_timer")
    }
}
