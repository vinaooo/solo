package io.github.vinaooo.solo.domain.scoring

fun interface ScoringStrategy {
    fun pointsFor(event: ScoreEvent): Int
}
