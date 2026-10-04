package io.github.vinaooo.solo.domain.hint

import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.KlondikeRules
import io.github.vinaooo.solo.domain.rules.RuleSet
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Tells whether a game is stuck: no sequence of moves can put another card on a foundation or turn a face-down card
 * up. It searches every position reachable by drawing, recycling and moving cards between columns (taking cards back
 * off the foundations doesn't count). If the search grows past [maxPositions] it gives up and says not stuck, so it
 * never claims a dead end that isn't one. A dead end takes the whole search, so callers run it in the background; it
 * stops as soon as its coroutine is cancelled.
 */
class DeadEndDetector(private val rules: RuleSet = KlondikeRules(), private val maxPositions: Int = MAX_POSITIONS) {

    suspend fun isStuck(state: GameState): Boolean {
        if (state.isWon) return false
        val foundationCards = state.foundationCards()
        val faceDownCards = state.faceDownCards()
        val seen = HashSet<GameState>()
        val queue = ArrayDeque(listOf(state))
        while (queue.isNotEmpty()) {
            val position = queue.removeFirst()
            if (!seen.add(position.board())) continue
            if (seen.size > maxPositions) return false
            currentCoroutineContext().ensureActive()
            for (move in rules.legalMoves(position)) {
                if (move is Move.FoundationToTableau) continue
                val next = rules.perform(position, move).state
                if (next.foundationCards() > foundationCards || next.faceDownCards() < faceDownCards) return false
                queue.addLast(next)
            }
        }
        return true
    }

    /**
     * The cards alone: two positions that differ only in score, moves, time or recycles are the same position. With
     * limited passes (Vegas), the passes used are part of the position: they decide whether the stock comes back.
     */
    private fun GameState.board() = copy(
        score = 0,
        moves = 0,
        recycles = if (mode.recycleLimit(drawMode) == null) 0 else recycles,
        elapsedSeconds = 0,
    )

    private fun GameState.foundationCards() = foundations.sumOf { it.size }

    private fun GameState.faceDownCards() = tableau.sumOf { column -> column.count { !it.isFaceUp } }

    private companion object {
        const val MAX_POSITIONS = 5_000
    }
}
