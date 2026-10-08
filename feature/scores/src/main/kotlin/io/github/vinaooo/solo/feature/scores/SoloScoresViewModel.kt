package io.github.vinaooo.solo.feature.scores

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.key
import io.github.vinaooo.solo.domain.model.ranking
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.scores.ScoresViewModel
import javax.inject.Inject

/** vinkit's Scores, a tab per Solo mode played, each ranked its own way (cumulative Vegas: stats only). */
@HiltViewModel
class SoloScoresViewModel @Inject constructor(scores: ScoreRepository, stats: StatsRepository) :
    ScoresViewModel(
        scores,
        stats,
        GameMode.entries.map {
            it.key
        },
        rankingFor = { enumValueOf<GameMode>(it).ranking() },
    )
