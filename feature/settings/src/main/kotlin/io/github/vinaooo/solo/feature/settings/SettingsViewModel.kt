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
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ThemeColor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A change to Solo's own settings: each one starts a new game, after a confirmation if one is in progress. */
sealed interface SettingsChange {
    fun applyTo(settings: Settings): Settings

    data class DrawModeChanged(val value: DrawMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(drawMode = value)
    }

    data class DifficultyChanged(val value: Difficulty) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(difficulty = value)
    }

    data class GameModeChanged(val value: GameMode) : SettingsChange {
        override fun applyTo(settings: Settings) = settings.copy(gameMode = value)
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appRepository: AppSettingsRepository,
    private val resumeGame: ResumeGame,
) : ViewModel() {

    val settings: StateFlow<Settings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Settings())

    val appSettings: StateFlow<AppSettings> = appRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            AppSettings(themeColor = ThemeColor.GREEN),
        )

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

    fun onAppChange(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { appRepository.update(transform) }
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
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
