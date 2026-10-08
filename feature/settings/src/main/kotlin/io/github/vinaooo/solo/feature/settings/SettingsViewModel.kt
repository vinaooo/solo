package io.github.vinaooo.solo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsChange {
    fun applyTo(settings: Settings): Settings

    data class DrawModeChanged(val value: DrawMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(drawMode = value)
    }

    data class DifficultyChanged(val value: Difficulty) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(difficulty = value)
    }

    data class ThemeModeChanged(val value: ThemeMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(themeMode = value)
    }

    data class DynamicColorChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(dynamicColor = value)
    }

    data class ThemeColorChanged(val value: ThemeColor) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(themeColor = value)
    }

    data class SoundChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(soundEnabled = value)
    }

    data class HapticsChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(hapticsEnabled = value)
    }

    data class HandednessChanged(val value: Handedness) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(handedness = value)
    }

    data class BoardAlignmentChanged(val value: BoardAlignment) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(boardAlignment = value)
    }

    data class GameModeChanged(val value: GameMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(gameMode = value)
    }

    data class PhoneViewChanged(val value: Boolean) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(phoneView = value)
    }

    data class PhoneViewSideChanged(val value: PhoneViewSide) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(phoneViewSide = value)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val resumeGame: ResumeGame,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

    private val pending = MutableStateFlow<SettingsChange?>(null)

    /**
     * A draw mode, game mode or difficulty change waiting for the player to confirm abandoning the game in progress,
     * which the switch would end.
     */
    val pendingChange: StateFlow<SettingsChange?> = pending.asStateFlow()

    fun onChange(change: SettingsChange) {
        viewModelScope.launch {
            if (startsNewGame(change) && resumeGame()?.isInProgress == true) {
                pending.value = change
            } else {
                repository.update(change::applyTo)
            }
        }
    }

    fun confirmChange() {
        val change = pending.value ?: return
        pending.value = null
        viewModelScope.launch { repository.update(change::applyTo) }
    }

    fun dismissChange() {
        pending.value = null
    }

    private fun startsNewGame(change: SettingsChange): Boolean = when (change) {
        is SettingsChange.DrawModeChanged -> change.value != settings.value.drawMode
        is SettingsChange.GameModeChanged -> change.value != settings.value.gameMode
        is SettingsChange.DifficultyChanged -> change.value != settings.value.difficulty
        else -> false
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
