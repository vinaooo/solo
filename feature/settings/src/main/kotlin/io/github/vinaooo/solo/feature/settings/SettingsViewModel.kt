package io.github.vinaooo.solo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsChange {
    fun applyTo(settings: Settings): Settings

    data class DrawModeChanged(val value: DrawMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(drawMode = value)
    }

    data class ThemeModeChanged(val value: ThemeMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(themeMode = value)
    }

    data class DynamicColorChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(dynamicColor = value)
    }

    data class SoundChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(soundEnabled = value)
    }

    data class HapticsChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(hapticsEnabled = value)
    }

    data class ShowTimerChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(showTimer = value)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

    fun onChange(change: SettingsChange) {
        viewModelScope.launch { repository.update(change::applyTo) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
