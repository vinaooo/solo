package io.github.vinaooo.solo.feature.scores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.usecase.ObserveRankedModes
import io.github.vinaooo.solo.domain.usecase.ObserveStats
import io.github.vinaooo.solo.domain.usecase.ObserveTopScores
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ScoresUiState(
    val scores: List<ScoreRecord> = emptyList(),
    val stats: GameStats = GameStats(),
    val isLoading: Boolean = true,
    /** The modes already played to a ranked score, one tab each. */
    val modes: List<GameMode> = emptyList(),
    /** The mode whose ranking shows. */
    val mode: GameMode = GameMode.STANDARD,
)

@HiltViewModel
class ScoresViewModel @Inject constructor(
    observeTopScores: ObserveTopScores,
    observeStats: ObserveStats,
    observeRankedModes: ObserveRankedModes,
) : ViewModel() {
    private val chosen = MutableStateFlow<GameMode?>(null)

    // The chosen tab, or the first mode with scores until one is chosen.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ScoresUiState> =
        combine(observeRankedModes(), chosen) { played, choice ->
            val modes = GameMode.entries.filter { it in played }
            modes to (choice?.takeIf { it in modes } ?: modes.firstOrNull() ?: GameMode.STANDARD)
        }.flatMapLatest { (modes, mode) ->
            combine(observeTopScores(mode), observeStats()) { scores, stats ->
                ScoresUiState(scores, stats, isLoading = false, modes = modes, mode = mode)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScoresUiState())

    fun selectMode(mode: GameMode) {
        chosen.value = mode
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
