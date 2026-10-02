package io.github.vinaooo.solo.domain.session

import io.github.vinaooo.solo.domain.history.UndoHistory
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import kotlinx.serialization.Serializable

/** A game being played: the deal's seed, the current board and its undo history. This is what gets saved. */
@Serializable
data class GameSession(val seed: Long, val state: GameState, val history: UndoHistory = UndoHistory()) {
    val canUndo: Boolean get() = history.canUndo

    val canRedo: Boolean get() = history.canRedo

    val isInProgress: Boolean get() = state.moves > 0 && !state.isWon

    fun play(move: Move, engine: GameEngine): GameSession? = when (val outcome = engine.apply(state, move)) {
        is MoveOutcome.Applied -> copy(state = outcome.state, history = history.push(state, outcome.state))
        MoveOutcome.Rejected -> null
    }

    fun undo(): GameSession? = history.undo(state)?.let { (restored, remaining) ->
        copy(state = restored, history = remaining)
    }

    fun redo(): GameSession? = history.redo(state)?.let { (replayed, remaining) ->
        copy(state = replayed, history = remaining)
    }

    fun tick(elapsedSeconds: Long, engine: GameEngine): GameSession = copy(state = engine.tick(state, elapsedSeconds))
}
