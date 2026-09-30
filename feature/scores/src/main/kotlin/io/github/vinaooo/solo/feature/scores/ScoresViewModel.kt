package io.github.vinaooo.solo.feature.scores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.usecase.ObserveStats
import io.github.vinaooo.solo.domain.usecase.ObserveTopScores
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ScoresUiState(
    val scores: List<ScoreRecord> = emptyList(),
    val stats: GameStats = GameStats(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ScoresViewModel @Inject constructor(observeTopScores: ObserveTopScores, observeStats: ObserveStats) :
    ViewModel() {
    val uiState: StateFlow<ScoresUiState> =
        combine(observeTopScores(), observeStats()) { scores, stats -> ScoresUiState(scores, stats, isLoading = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ScoresUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
