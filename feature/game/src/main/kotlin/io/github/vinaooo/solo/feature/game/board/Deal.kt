package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.Animatable
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * The deal of a new game, played on the board. Every card gathers face down into a deck in the middle of the
 * tableau; then the cards leave it one by one, column after column (column 1's card, column 2's face-down card and
 * then its top card, and so on to column 7's six face-down cards and its top card), and the rest go to the stock.
 * Last, the columns' top cards flip face up together.
 */
@Stable
internal class Deal {
    /** From the gathering to the flip: the board shows every card as a card, face down, and takes no taps. */
    var active by mutableStateOf(false)
        private set

    /** The cards still in the deck, with their place in the deal. */
    private val held = mutableStateMapOf<CardIdentity, Int>()
    private val flip = Animatable(1f)
    var deck = IntOffset.Zero

    fun shows(placed: PlacedCard) = active || placed.card.isFaceUp || placed.pile !is PileRef.Tableau

    fun target(placed: PlacedCard): IntOffset = if (placed.card.identity() in
        held
    ) {
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

    /** Face down for the whole deal, until the flip at the end turns the face-up cards over halfway through. */
    fun face(card: Card): Card = if (active && (!card.isFaceUp || flip.value < HALF)) card.faceDown() else card

    /** A face-up card narrows to its edge and widens again as it flips. */
    fun flipScale(card: Card): Float = if (active && card.isFaceUp) abs(cos(PI.toFloat() * flip.value)) else 1f

    suspend fun play(order: List<CardIdentity>) {
        // Timed like an animation, not with delay(), so the device's animation speed stretches the pauses as it
        // stretches the cards' flights (and the deal is immediate when animations are off).
        suspend fun pause(millis: Long) =
            animate(0f, 1f, animationSpec = tween(millis.toInt(), easing = LinearEasing)) { _, _ -> }
        flip.snapTo(0f)
        order.forEachIndexed { i, card -> held[card] = i }
        active = true
        pause(GATHER_MILLIS)
        order.forEachIndexed { i, card ->
            held.remove(card)
            pause(if (i < TABLEAU_CARDS) TABLEAU_STEP_MILLIS else STOCK_STEP_MILLIS)
        }
        pause(SETTLE_MILLIS)
        flip.animateTo(1f)
        active = false
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
        deal.play(dealOrder(currentState))
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

/** Column by column, each column's face-down cards before its top card, then the stock from the bottom up. */
internal fun dealOrder(state: GameState): List<CardIdentity> =
    state.tableau.flatMap { column -> column.map { it.identity() } } + state.stock.map { it.identity() }

private const val HALF = 0.5f
private const val TABLEAU_CARDS = 28
private const val DECK_Z = 5_000f
private const val GUARD_Z = 20_000f
private const val GATHER_MILLIS = 500L
private const val TABLEAU_STEP_MILLIS = 80L
private const val STOCK_STEP_MILLIS = 20L
private const val SETTLE_MILLIS = 300L
