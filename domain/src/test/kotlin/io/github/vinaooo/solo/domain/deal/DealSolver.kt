package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.rules.KlondikeRules
import io.github.vinaooo.solo.domain.rules.RuleSet
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Proves a deal winnable by finding a win: a depth-first search over the game's own [rules], so a win it finds is one
 * the player can play, with its pass limit and draw mode. It tries only the moves that make progress (see
 * [DealMoves]), so it may miss a win, never invent one. It gives up past [maxPositions].
 */
class DealSolver(private val rules: RuleSet = KlondikeRules()) {

    /** The positions it took to find a win, or null when it found none within [maxPositions]. */
    suspend fun positionsToWin(state: GameState, maxPositions: Int): Int? {
        val seen = HashSet<String>()
        val stack = ArrayDeque(listOf(state))
        while (stack.isNotEmpty()) {
            val position = stack.removeLast()
            if (position.isWon) return seen.size
            if (!seen.add(position.key())) continue
            if (seen.size > maxPositions) return null
            if (seen.size % CANCEL_CHECK_EVERY == 0) currentCoroutineContext().ensureActive()
            // Pushed in reverse, so the most promising move is searched first.
            DealMoves.of(position, rules).asReversed().forEach { stack.addLast(rules.perform(position, it).state) }
        }
        return null
    }

    private companion object {
        const val CANCEL_CHECK_EVERY = 1_024
    }
}

/** The moves worth searching, best first. Leaving one out can only hide a win, never make a deal look winnable. */
internal object DealMoves {

    fun of(state: GameState, rules: RuleSet): List<Move> {
        val legal = candidates(state).filter { rules.isLegal(state, it) }
        // A safe card on a foundation can never hurt, so it is the only move tried.
        legal.firstOrNull { it.isSafeToFoundation(state) }?.let { return listOf(it) }
        return legal.sortedBy { it.priority(state) }
    }

    /** Like [RuleSet.legalMoves], but only the moves that can make progress, and only the run that fits each column. */
    private fun candidates(state: GameState): List<Move> = buildList {
        add(Move.Draw)
        add(Move.Recycle)
        for (to in foundations) add(Move.WasteToFoundation(to))
        for (to in tableau) add(Move.WasteToTableau(to))
        for (from in tableau) {
            for (to in foundations) add(Move.TableauToFoundation(from, to))
            for (to in tableau) {
                val count = state.runFitting(from, to) ?: continue
                if (Move.TableauToTableau(from, to, count).isUseful(state)) add(Move.TableauToTableau(from, to, count))
            }
        }
    }

    /** How many cards of column [from] go onto column [to]: the run headed by the one card that fits there. */
    private fun GameState.runFitting(from: Int, to: Int): Int? {
        if (from == to) return null
        val source = tableau[from]
        val top = tableau[to].lastOrNull()
        val head = source.indexOfLast { card ->
            card.isFaceUp && if (top == null) card.rank == Rank.KING else card.fitsOn(top)
        }
        return if (head < 0) null else source.size - head
    }

    private fun Move.TableauToTableau.isUseful(state: GameState): Boolean {
        val below = state.tableau[from].cardUnder(count)
        return when {
            // The whole run: turns a card up, or empties the column for a king (not a king already at the bottom).
            below == null -> state.tableau[to].isNotEmpty()
            !below.isFaceUp -> true
            // Part of a run: only to free the card under it for a foundation.
            else -> state.foundations.any { below.goesOn(it) }
        }
    }

    private fun Move.priority(state: GameState): Int = when (this) {
        is Move.TableauToFoundation, is Move.WasteToFoundation -> 0
        is Move.TableauToTableau -> if (state.tableau[from].cardUnder(count)?.isFaceUp == false) 1 else 3
        is Move.WasteToTableau -> 2
        else -> 4
    }

    /** A card no tableau card could still need to sit on: both foundations of the other color are a rank below. */
    private fun Move.isSafeToFoundation(state: GameState): Boolean {
        val card = when (this) {
            is Move.WasteToFoundation -> state.waste.last()
            is Move.TableauToFoundation -> state.tableau[from].last()
            else -> return false
        }
        if (card.rank.value <= 2) return true
        val opposite = state.foundations.filter { pile -> pile.firstOrNull()?.hasOppositeColorOf(card) == true }
        return opposite.size == 2 && opposite.all { it.size >= card.rank.value - 1 }
    }

    private val tableau = 0 until GameState.TABLEAU_COUNT
    private val foundations = 0 until GameState.FOUNDATION_COUNT

    /** The card under the top [count] cards of a column. */
    private fun List<Card>.cardUnder(count: Int): Card? = getOrNull(size - count - 1)

    private fun Card.fitsOn(top: Card) = rank.isOneBelow(top.rank) && hasOppositeColorOf(top)

    private fun Card.goesOn(foundation: List<Card>): Boolean {
        val top = foundation.lastOrNull() ?: return rank == Rank.ACE
        return suit == top.suit && top.rank.isOneBelow(rank)
    }
}

/**
 * The cards alone, with the columns in any order (which column holds what doesn't change whether the game can be
 * won), and the passes used when they are limited.
 */
private fun GameState.key(): String = buildString {
    tableau.map { column -> column.joinToString("") { it.code() } }.sorted().forEach { append(it).append('|') }
    stock.forEach { append(it.code()) }
    append('|')
    waste.forEach { append(it.code()) }
    append('|')
    foundations.map { it.size * 4 + (it.firstOrNull()?.suit?.ordinal ?: 0) }.sorted().forEach { append(it).append(',') }
    if (mode.recycleLimit(drawMode) != null) append(recycles)
}

private fun Card.code(): String {
    val index = suit.ordinal * Rank.entries.size + rank.ordinal
    return (FIRST_CODE + index + if (isFaceUp) FACE_UP else 0).toChar().toString()
}

private const val FIRST_CODE = 48
private const val FACE_UP = 64
