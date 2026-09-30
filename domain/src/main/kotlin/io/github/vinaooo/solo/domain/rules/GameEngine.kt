package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.scoring.ScoreEvent
import io.github.vinaooo.solo.domain.scoring.ScoringStrategy
import io.github.vinaooo.solo.domain.scoring.StandardScoring

sealed interface MoveOutcome {
    data class Applied(val state: GameState, val events: List<ScoreEvent>) : MoveOutcome

    data object Rejected : MoveOutcome
}

/** Pure game loop: validates moves, applies them and keeps the score. */
class GameEngine(
    private val rules: RuleSet = KlondikeRules(),
    private val scoring: ScoringStrategy = StandardScoring(),
) {
    fun apply(state: GameState, move: Move): MoveOutcome {
        if (!rules.isLegal(state, move)) return MoveOutcome.Rejected
        val transition = rules.perform(state, move)
        val next = transition.state.copy(moves = state.moves + 1)
        val events = transition.events + listOfNotNull(ScoreEvent.Won(next.elapsedSeconds).takeIf { next.isWon })
        return MoveOutcome.Applied(next.withPoints(events), events)
    }

    /** Advances the clock, charging the time penalty for each completed ten-second period. */
    fun tick(state: GameState, elapsedSeconds: Long): GameState {
        if (state.isWon || elapsedSeconds <= state.elapsedSeconds) return state
        val periods = elapsedSeconds / PENALTY_PERIOD_SECONDS - state.elapsedSeconds / PENALTY_PERIOD_SECONDS
        val ticked = state.copy(elapsedSeconds = elapsedSeconds)
        return if (periods > 0) ticked.withPoints(listOf(ScoreEvent.TimeElapsed(periods))) else ticked
    }

    fun legalMoves(state: GameState): List<Move> = rules.legalMoves(state)

    fun isLegal(state: GameState, move: Move): Boolean = rules.isLegal(state, move)

    private fun GameState.withPoints(events: List<ScoreEvent>): GameState =
        copy(score = (score + events.sumOf(scoring::pointsFor)).coerceAtLeast(0))

    private companion object {
        const val PENALTY_PERIOD_SECONDS = 10L
    }
}
