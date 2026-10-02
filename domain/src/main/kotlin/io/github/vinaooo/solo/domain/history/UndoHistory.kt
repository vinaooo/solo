package io.github.vinaooo.solo.domain.history

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.scoring.ScoreEvent
import io.github.vinaooo.solo.domain.scoring.ScoringStrategy
import io.github.vinaooo.solo.domain.scoring.StandardScoring
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class UndoEntry(val previous: GameState, val scoreDelta: Int)

/**
 * Unlimited, immutable undo and redo stacks. Undo restores the previous board but keeps the clock running,
 * takes back the points the move earned and charges the undo penalty. Redo plays the undone move again: it
 * earns the move's points back and counts as a move, but the undo penalty stays. A new move clears the redo stack.
 */
@Serializable
data class UndoHistory(val entries: List<UndoEntry> = emptyList(), val redos: List<UndoEntry> = emptyList()) {
    @Transient
    private val scoring: ScoringStrategy = StandardScoring()

    val canUndo: Boolean get() = entries.isNotEmpty()

    val canRedo: Boolean get() = redos.isNotEmpty()

    val size: Int get() = entries.size

    fun push(before: GameState, after: GameState): UndoHistory =
        UndoHistory(entries + UndoEntry(before, after.score - before.score))

    fun undo(current: GameState): Pair<GameState, UndoHistory>? {
        val last = entries.lastOrNull() ?: return null
        val score = current.score - last.scoreDelta + scoring.pointsFor(ScoreEvent.Undo)
        val restored = last.previous.copy(
            score = score.coerceAtLeast(0),
            moves = current.moves,
            elapsedSeconds = current.elapsedSeconds,
        )
        return restored to UndoHistory(entries.dropLast(1), redos + UndoEntry(current, last.scoreDelta))
    }

    /** [UndoEntry.previous] of a redo entry is the board the undone move had produced. */
    fun redo(current: GameState): Pair<GameState, UndoHistory>? {
        val next = redos.lastOrNull() ?: return null
        val replayed = next.previous.copy(
            score = (current.score + next.scoreDelta).coerceAtLeast(0),
            moves = current.moves + 1,
            elapsedSeconds = current.elapsedSeconds,
        )
        return replayed to UndoHistory(entries + UndoEntry(current, next.scoreDelta), redos.dropLast(1))
    }
}
