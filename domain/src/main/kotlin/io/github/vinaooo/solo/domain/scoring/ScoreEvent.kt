package io.github.vinaooo.solo.domain.scoring

import io.github.vinaooo.solo.domain.model.DrawMode

/** Things that happen during play that a [ScoringStrategy] may reward or penalize. */
sealed interface ScoreEvent {
    data object WasteToTableau : ScoreEvent

    data object WasteToFoundation : ScoreEvent

    data object TableauToFoundation : ScoreEvent

    data object FoundationToTableau : ScoreEvent

    data object CardRevealed : ScoreEvent

    data object Undo : ScoreEvent

    data class Recycle(val drawMode: DrawMode, val recycleNumber: Int) : ScoreEvent

    data class TimeElapsed(val tenSecondPeriods: Long) : ScoreEvent

    data class Won(val elapsedSeconds: Long) : ScoreEvent
}
