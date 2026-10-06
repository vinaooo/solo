package io.github.vinaooo.solo.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
            gameMode = enumOrDefault(this[Keys.GAME_MODE], defaults.gameMode),
            difficulty = enumOrDefault(this[Keys.DIFFICULTY], defaults.difficulty),
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            themeColor = enumOrDefault(this[Keys.THEME_COLOR], defaults.themeColor),
            soundEnabled = this[Keys.SOUND] ?: defaults.soundEnabled,
            hapticsEnabled = this[Keys.HAPTICS] ?: defaults.hapticsEnabled,
            handedness = enumOrDefault(this[Keys.HANDEDNESS], defaults.handedness),
            boardAlignment = enumOrDefault(this[Keys.BOARD_ALIGNMENT], defaults.boardAlignment),
            phoneView = this[Keys.PHONE_VIEW] ?: defaults.phoneView,
            phoneViewSide = enumOrDefault(this[Keys.PHONE_VIEW_SIDE], defaults.phoneViewSide),
            autoCompleteTipsShown = this[Keys.AUTO_COMPLETE_TIPS_SHOWN] ?: defaults.autoCompleteTipsShown,
        )
    }

    private fun MutablePreferences.write(settings: Settings) {
        this[Keys.DRAW_MODE] = settings.drawMode.name
        this[Keys.GAME_MODE] = settings.gameMode.name
        this[Keys.DIFFICULTY] = settings.difficulty.name
        this[Keys.THEME_MODE] = settings.themeMode.name
        this[Keys.DYNAMIC_COLOR] = settings.dynamicColor
        this[Keys.THEME_COLOR] = settings.themeColor.name
        this[Keys.SOUND] = settings.soundEnabled
        this[Keys.HAPTICS] = settings.hapticsEnabled
        this[Keys.HANDEDNESS] = settings.handedness.name
        this[Keys.BOARD_ALIGNMENT] = settings.boardAlignment.name
        this[Keys.PHONE_VIEW] = settings.phoneView
        this[Keys.PHONE_VIEW_SIDE] = settings.phoneViewSide.name
        this[Keys.AUTO_COMPLETE_TIPS_SHOWN] = settings.autoCompleteTipsShown
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val DRAW_MODE = stringPreferencesKey("draw_mode")
        val GAME_MODE = stringPreferencesKey("game_mode")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTICS = booleanPreferencesKey("haptics")
        val HANDEDNESS = stringPreferencesKey("handedness")
        val BOARD_ALIGNMENT = stringPreferencesKey("board_alignment")
        val PHONE_VIEW = booleanPreferencesKey("phone_view")
        val PHONE_VIEW_SIDE = stringPreferencesKey("phone_view_side")
        val AUTO_COMPLETE_TIPS_SHOWN = intPreferencesKey("auto_complete_tips_shown")
    }
}
