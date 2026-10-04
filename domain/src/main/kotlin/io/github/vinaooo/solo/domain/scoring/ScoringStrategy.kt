package io.github.vinaooo.solo.domain.scoring

import io.github.vinaooo.solo.domain.model.GameMode

fun interface ScoringStrategy {
    fun pointsFor(event: ScoreEvent): Int

    /** The score as the strategy allows it: points never go below zero, unless overridden (Vegas money does). */
    fun bounded(score: Int): Int = score.coerceAtLeast(0)
}

/** The scoring each [GameMode] plays with. */
fun scoringFor(mode: GameMode): ScoringStrategy = when (mode) {
    GameMode.STANDARD, GameMode.COUNTER_TIME -> StandardScoring()
    GameMode.VEGAS, GameMode.VEGAS_CUMULATIVE -> VegasScoring()
}
