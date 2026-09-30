package io.github.vinaooo.solo.domain.scoring

import io.github.vinaooo.solo.domain.model.DrawMode

/** Windows-style standard scoring. */
class StandardScoring : ScoringStrategy {
    override fun pointsFor(event: ScoreEvent): Int = when (event) {
        ScoreEvent.WasteToTableau -> TO_TABLEAU
        ScoreEvent.WasteToFoundation, ScoreEvent.TableauToFoundation -> TO_FOUNDATION
        ScoreEvent.CardRevealed -> CARD_REVEALED
        ScoreEvent.FoundationToTableau -> FOUNDATION_TO_TABLEAU
        ScoreEvent.Undo -> UNDO
        is ScoreEvent.Recycle -> recyclePenalty(event)
        is ScoreEvent.TimeElapsed -> (event.tenSecondPeriods * TIME_PENALTY).toInt()
        is ScoreEvent.Won -> winBonus(event.elapsedSeconds)
    }

    private fun recyclePenalty(event: ScoreEvent.Recycle): Int = when (event.drawMode) {
        DrawMode.ONE -> DRAW_ONE_RECYCLE_PENALTY
        DrawMode.THREE -> if (event.recycleNumber >= FREE_DRAW_THREE_PASSES) DRAW_THREE_RECYCLE_PENALTY else 0
    }

    private fun winBonus(elapsedSeconds: Long): Int =
        if (elapsedSeconds < MIN_SECONDS_FOR_BONUS) 0 else (WIN_BONUS_NUMERATOR / elapsedSeconds).toInt()

    private companion object {
        const val TO_TABLEAU = 5
        const val TO_FOUNDATION = 10
        const val CARD_REVEALED = 5
        const val FOUNDATION_TO_TABLEAU = -15
        const val UNDO = -15
        const val TIME_PENALTY = -2L
        const val DRAW_ONE_RECYCLE_PENALTY = -100
        const val DRAW_THREE_RECYCLE_PENALTY = -20

        /** Recycle #3 starts the 4th pass, the first one that is charged. */
        const val FREE_DRAW_THREE_PASSES = 3
        const val MIN_SECONDS_FOR_BONUS = 30L
        const val WIN_BONUS_NUMERATOR = 700_000L
    }
}
