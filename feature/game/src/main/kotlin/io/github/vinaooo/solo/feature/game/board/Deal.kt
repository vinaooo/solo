package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef
import kotlin.math.roundToInt

/**
 * The deal of a new game, played on the board. The cards gather face down into a deck in the middle of the tableau
 * as the old game's bars shrink away; then, column after column, the column's bar of face-down cards grows from
 * nothing and its top card slides from the deck onto it, face up; last, the rest of the deck slides to the stock.
 */
@Stable
internal class Deal {
    /** From the gathering to the last card: the board takes no taps, and the stock isn't there yet. */
    var active by mutableStateOf(false)
        private set

    /** The cards still in the deck, with their place in the deal. */
    private val held = mutableStateMapOf<CardIdentity, Int>()

    /** How many columns have their bar of face-down cards so far. */
    private var barred by mutableIntStateOf(Int.MAX_VALUE)
    var deck = IntOffset.Zero

    fun shows(placed: PlacedCard) = placed.card.isFaceUp || placed.pile !is PileRef.Tableau

    fun hasBar(column: Int) = column < barred

    /** Into the deck: the cards still in it, and, while dealing, the columns' face-down cards (bars to come). */
    fun target(placed: PlacedCard): IntOffset = if (placed.card.identity() in held || (active && !shows(placed))) {
        deck
    } else {
        IntOffset(placed.position.x.roundToInt(), placed.position.y.roundToInt())
    }

    /**
     * In the deck, the next card to leave on top; leaving it, above the board, the last to leave on top; elsewhere,
     * where the board puts it ([otherwise]).
     */
    fun zIndex(placed: PlacedCard, moving: Boolean, otherwise: Float): Float {
        val order = held[placed.card.identity()]
        return when {
            order != null -> DECK_Z + held.size - order
            active && moving -> LIFTED_Z + placed.z
            else -> otherwise
        }
    }

    /**
     * The face it shows: down in the deck and, while dealing, on the way to a column's bar; otherwise up when the
     * board wants its face shown ([showFace]) or as it lies.
     */
    fun face(placed: PlacedCard, showFace: Boolean): Card = when {
        placed.card.identity() in held || (active && !shows(placed)) -> placed.card.faceDown()
        showFace -> placed.card.faceUp()
        else -> placed.card
    }

    suspend fun play(state: GameState) {
        // Timed like an animation, not with delay(), so the device's animation speed stretches the pauses as it
        // stretches the cards' flights (and the deal is immediate when animations are off).
        suspend fun pause(millis: Long) =
            animate(0f, 1f, animationSpec = tween(millis.toInt(), easing = LinearEasing)) { _, _ -> }
        // The columns' face-down cards come with their bars, not from the deck: only the top cards and the stock fly.
        val tops = state.tableau.map { it.last().identity() }
        val stock = state.stock.map { it.identity() }
        (tops + stock).forEachIndexed { i, card -> held[card] = i }
        barred = 0
        active = true
        pause(GATHER_MILLIS)
        state.tableau.forEachIndexed { column, cards ->
            barred = column + 1
            if (cards.size > 1) pause(BAR_MILLIS)
            held.remove(tops[column])
            pause(TABLEAU_STEP_MILLIS)
        }
        stock.forEach { card ->
            held.remove(card)
            pause(STOCK_STEP_MILLIS)
        }
        pause(SETTLE_MILLIS)
        active = false
        barred = Int.MAX_VALUE
    }
}

/**
 * Plays each new deal once: [deals] counts the games dealt, and the board remembers how many it played, across
 * rotations and trips to the other screens (a draw mode changed in Settings is dealt on coming back).
 */
@Composable
internal fun rememberDeal(deals: Int, state: GameState, layout: BoardLayout): Deal {
    val deal = remember { Deal() }
    // In the middle of the tableau's area.
    val tableauTop = layout.slot(PileRef.Tableau(0)).y
    deal.deck = IntOffset(
        ((layout.width - layout.cardWidth) / 2).roundToInt(),
        (tableauTop + (layout.height - tableauTop - layout.cardHeight) / 2).roundToInt(),
    )
    val currentState by rememberUpdatedState(state)
    var played by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(deals) {
        if (deals <= played) return@LaunchedEffect
        played = deals
        deal.play(currentState)
    }
    return deal
}

/** While the deal plays, the board takes no taps or drags: a move would cut it short. */
@Composable
internal fun BoxScope.DealGuard(deal: Deal) {
    if (!deal.active) return
    Box(
        Modifier
            .matchParentSize()
            .zIndex(GUARD_Z)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
    )
}

private const val DECK_Z = 5_000f
private const val GUARD_Z = 20_000f
private const val GATHER_MILLIS = 500L
private const val BAR_MILLIS = 50L
private const val TABLEAU_STEP_MILLIS = 60L
private const val STOCK_STEP_MILLIS = 20L
private const val SETTLE_MILLIS = 300L
