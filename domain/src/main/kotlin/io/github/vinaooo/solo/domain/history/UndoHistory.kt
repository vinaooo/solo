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
 * Unlimited, immutable undo stack. Undo restores the previous board but keeps the clock running,
 * takes back the points the move earned and charges the undo penalty.
 */
@Serializable
data class UndoHistory(val entries: List<UndoEntry> = emptyList()) {
    @Transient
    private val scoring: ScoringStrategy = StandardScoring()

    val canUndo: Boolean get() = entries.isNotEmpty()

    val size: Int get() = entries.size

    fun push(before: GameState, after: GameState): UndoHistory =
        copy(entries = entries + UndoEntry(before, after.score - before.score))

    fun undo(current: GameState): Pair<GameState, UndoHistory>? {
        val last = entries.lastOrNull() ?: return null
        val score = current.score - last.scoreDelta + scoring.pointsFor(ScoreEvent.Undo)
        val restored = last.previous.copy(
            score = score.coerceAtLeast(0),
            moves = current.moves,
            elapsedSeconds = current.elapsedSeconds,
        )
        return restored to copy(entries = entries.dropLast(1))
    }
}
