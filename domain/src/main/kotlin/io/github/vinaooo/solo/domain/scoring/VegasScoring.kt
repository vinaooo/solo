package io.github.vinaooo.solo.domain.scoring

/**
 * Vegas scoring, in dollars: $5 for each card that reaches a foundation, taken back if it leaves one (or moving a
 * card up and down would make endless money). Nothing else counts, not time, undo or recycling, and the score may
 * be negative: a game starts at -$52, its cost.
 */
class VegasScoring : ScoringStrategy {
    override fun pointsFor(event: ScoreEvent): Int = when (event) {
        ScoreEvent.WasteToFoundation, ScoreEvent.TableauToFoundation -> PER_CARD
        ScoreEvent.FoundationToTableau -> -PER_CARD
        ScoreEvent.WasteToTableau,
        ScoreEvent.CardRevealed,
        ScoreEvent.Undo,
        is ScoreEvent.Recycle,
        is ScoreEvent.TimeElapsed,
        is ScoreEvent.Won,
        -> 0
    }

    override fun bounded(score: Int): Int = score

    private companion object {
        const val PER_CARD = 5
    }
}
