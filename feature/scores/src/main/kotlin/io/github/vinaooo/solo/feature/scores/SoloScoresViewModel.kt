package io.github.vinaooo.solo.feature.scores

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.key
import io.github.vinaooo.solo.domain.model.ranking
import io.github.vinaooo.solo.domain.repository.VegasBankRepository
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.scores.ScoresViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** vinkit's Scores, a tab per Solo mode played, each ranked its own way, and cumulative Vegas's [bank]. */
@HiltViewModel
class SoloScoresViewModel @Inject constructor(
    scores: ScoreRepository,
    stats: StatsRepository,
    vegasBank: VegasBankRepository,
) : ScoresViewModel(
    scores,
    stats,
    GameMode.entries.map {
        it.key
    },
    rankingFor = { enumValueOf<GameMode>(it).ranking() },
) {
    /** Cumulative Vegas's balance, in dollars. */
    val bank: StateFlow<Int> = vegasBank.bank.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), 0)

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
