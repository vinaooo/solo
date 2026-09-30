package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.scoring.ScoreEvent

/** The board after a move plus what happened, before scoring is applied. */
data class Transition(val state: GameState, val events: List<ScoreEvent> = emptyList())
