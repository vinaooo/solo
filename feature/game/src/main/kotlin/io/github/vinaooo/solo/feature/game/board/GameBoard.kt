package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
import io.github.vinaooo.solo.core.designsystem.component.EmptyPileSlot
import io.github.vinaooo.solo.core.designsystem.component.PlayingCard
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.feature.game.CardSpot
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.R
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DragState(val pile: PileRef, val index: Int, val offset: Offset)

@Composable
fun GameBoard(
    state: GameState,
    hint: Move?,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    destinations: Map<CardSpot, List<PileRef>> = emptyMap(),
    handedness: Handedness = Handedness.RIGHT,
    deals: Int = 0,
) {
    BoxWithConstraints(modifier = modifier.semantics { isTraversalGroup = true }) {
        val density = LocalDensity.current
        val layout = rememberBoardLayout(constraints.maxWidth, constraints.maxHeight, handedness)
        val cardWidth = with(density) { layout.cardWidth.toDp() }
        val highlighted = remember(state, hint) { hint?.let { hintedCards(state, it) }.orEmpty() }
        var drag by remember { mutableStateOf<DragState?>(null) }
        val currentState by rememberUpdatedState(state)
        val currentOnIntent by rememberUpdatedState(onIntent)

        val moving = remember { mutableStateMapOf<CardIdentity, Boolean>() }
        val placedCards = layout.positions(state).values
        val deal = rememberDeal(deals, state, layout)

        EmptySlots(state, layout, cardWidth, onIntent)
        FaceDownPiles(state, layout, cardWidth, highlighted, deal)

        placedCards.forEach { placed ->
            key(placed.card.identity()) {
                val isHighlighted = placed.card.identity() in highlighted
                val accessibility = cardAccessibility(
                    role = cardRole(state, placed),
                    placed = placed,
                    hinted = isHighlighted,
                    destinations = destinations[CardSpot(placed.pile, placed.index)].orEmpty(),
                    handedness = handedness,
                    onIntent = onIntent,
                )
                BoardCard(
                    placed = placed,
                    cardWidth = cardWidth,
                    highlighted = isHighlighted,
                    cover = layout.cover(state, placed)?.let { CardCover(it.edge, with(density) { it.strip.toDp() }) },
                    // The edge this card lies along on the card under it, where that card still shows.
                    shadow = placedCards.find { it.pile == placed.pile && it.index == placed.index - 1 }
                        ?.takeIf { it.position != placed.position }
                        ?.let { layout.cover(state, it)?.edge },
                    dragOffset = drag?.takeIf { it.pile == placed.pile && placed.index >= it.index }?.offset,
                    pileMoving = stillMoving(placedCards, moving)[placed.pile],
                    onMovingChange = { moving[placed.card.identity()] = it },
                    deal = deal,
                    accessibility = accessibility,
                    gestures = accessibility.modifier.then(
                        if (!isDraggable(state, placed)) {
                            Modifier
                        } else {
                            Modifier.cardDrag(
                                placed = placed,
                                onDragChange = { drag = it },
                                onDrop = { offset ->
                                    layout.dropTarget(currentState, placed, offset.x, offset.y)?.let { to ->
                                        currentOnIntent(GameIntent.Drop(placed.pile, placed.index, to))
                                    }
                                },
                            )
                        },
                    ),
                )
            }
        }
        DealGuard(deal)
    }
}

@Composable
private fun rememberBoardLayout(width: Int, height: Int, handedness: Handedness): BoardLayout {
    val density = LocalDensity.current
    return remember(width, height, density, handedness) {
        with(density) {
            BoardLayout(width.toFloat(), height.toFloat(), GAP.toPx(), handedness, COLUMN_GAP.toPx())
        }
    }
}

/** One card, animated to its slot, or following the finger while [dragOffset] is set. */
@Composable
private fun BoardCard(
    placed: PlacedCard,
    cardWidth: Dp,
    highlighted: Boolean,
    cover: CardCover?,
    shadow: CardCover.Edge?,
    dragOffset: Offset?,
    pileMoving: IntRange?,
    onMovingChange: (Boolean) -> Unit,
    accessibility: CardAccessibility,
    gestures: Modifier,
    deal: Deal,
) {
    val target = deal.target(placed)
    // A column's face-down cards are drawn as its bar instead, once dealt.
    val visible = deal.shows(placed)
    val animated = remember { Animatable(target, IntOffset.VectorConverter) }
    val stockReturn = rememberStockReturn(placed.pile)
    val spec = stockReturn.motion(
        placed.pile,
        MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>(),
        MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val shown = remember { ShownFlag(visible, visible, placed.pile) }
    val appearing = shown.appearing(visible)
    val dragging = dragOffset != null
    // Its position follows the finger, so a card let go continues from where it was dropped (to its new slot, or
    // back to its old one) instead of jumping back to its old slot first.
    val dragPosition = dragOffset?.let { target + IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
    // Moving until the spring settles, not when it first reaches the slot: springs overshoot and come back.
    LaunchedEffect(target, dragPosition) {
        if (dragPosition != null) {
            animated.snapTo(dragPosition)
            onMovingChange(false)
            return@LaunchedEffect
        }
        if (appearing) {
            animated.snapTo(target)
            // It may have been flying when this replaced that animation (a new deal right after another): it isn't
            // now, or it would stay lifted over everything, the stock's count included.
            onMovingChange(false)
            return@LaunchedEffect
        }
        // Only a card that is drawn: face-down cards shifting under their bar would otherwise lift their whole column,
        // a card just turned up over the one flying away from it.
        onMovingChange(shown.value)
        // A returning card turns face down as it reaches the stock, not after the spring's last wobble.
        animated.animateTo(target, spec) {
            if (abs(value.x - target.x) + abs(value.y - target.y) <= LANDED_PX) stockReturn.landed()
        }
        onMovingChange(false)
        stockReturn.settled()
        shown.arriving = false
    }
    // Its new slot is set but the animation hasn't started yet: already flying, from the first frame, so a card it
    // uncovers never shows on top of it.
    val departing = !appearing && animated.targetValue != target
    // Cards of its pile still on their way: it flies with them if one is under it, and waits for them if above.
    val lifted = placed.index >= (pileMoving?.first ?: Int.MAX_VALUE)
    val awaitingCover = placed.index < (pileMoving?.last ?: -1)
    val drawn = shown.drawn(visible, departing, animated.isRunning, awaitingCover)
    val flying = shown.flying(placed.pile, appearing, dragging, lifted || departing)
    SideEffect { shown.update(drawn, visible, placed.pile) }
    PlayingCard(
        card = deal.face(placed, showFace = stockReturn.showsFace(placed.pile) || !visible),
        highlighted = highlighted,
        contentDescription = accessibility.description,
        interactionSource = accessibility.touches,
        // A card turned face down but still drawn shows whole: the layout already counts it under its bar.
        cover = cover.takeIf { visible },
        shadow = shadow,
        modifier = Modifier
            .offset { dragPosition ?: animated.value }
            .zIndex(deal.zIndex(placed, animated.isRunning, otherwise = stockReturn.zIndex(placed, flying)))
            .scale(if (dragging) DRAG_SCALE else 1f)
            .alpha(if (drawn) 1f else 0f)
            .width(cardWidth)
            .testTag("card_${placed.card.suit}_${placed.card.rank}")
            .then(gestures),
    )
}

/**
 * Whether a card was drawn ([value]) and face up ([faceUp]), and its [pile], the last time it was composed (not
 * state: nothing redraws when they change), so a card turned up or down, or moved, keeps in step with the cards
 * moving around it.
 */
private class ShownFlag(var value: Boolean, var faceUp: Boolean, var pile: PileRef) {
    /** On its way to a new pile, from the composition that moved it there until its spring settles. */
    var arriving = false

    fun arriving(pile: PileRef) = arriving || pile != this.pile

    fun update(drawn: Boolean, visible: Boolean, pile: PileRef) {
        value = drawn
        faceUp = visible
        arriving = arriving(pile)
        this.pile = pile
    }

    /**
     * A card turned up is already in its place, under the card that uncovers it, instead of flying there (and over
     * that card) from where it lay face down. That holds even if it was still drawn, turned down by an undo whose
     * animation the new move cut short: it was face down all the same.
     */
    fun appearing(visible: Boolean) = visible && !faceUp

    /**
     * Flying above the other cards: dragged, or [moving] on its way to a new pile. Not a card only shifting within its
     * own pile (the waste's fan closing up as three new cards arrive over it, a column spacing out), which keeps its
     * place among the cards. Never while [appearing], even if its pile still counts as moving from a moment ago: it
     * belongs under the card leaving it.
     */
    fun flying(pile: PileRef, appearing: Boolean, dragging: Boolean, moving: Boolean) =
        dragging || (!appearing && arriving(pile) && moving)

    /**
     * A card turned face down (by an undo) stays drawn, face up, while it is still settling: about to slide or
     * [sliding] into its face-down place, or [awaitingCover] from the card coming back. It hides once covered, so it
     * never just vanishes.
     */
    fun drawn(visible: Boolean, departing: Boolean, sliding: Boolean, awaitingCover: Boolean) =
        visible || (value && (departing || sliding || awaitingCover))
}

/** Drags the card (and the cards on top of it); [onDrop] gets the total offset when the finger lifts. */
private fun Modifier.cardDrag(
    placed: PlacedCard,
    onDragChange: (DragState?) -> Unit,
    onDrop: (Offset) -> Unit,
): Modifier = pointerInput(placed.pile, placed.index) {
    var offset: Offset? = null
    detectDragGestures(
        onDragStart = { offset = null },
        onDrag = { change, amount ->
            change.consume()
            val moved = offset?.plus(amount) ?: amount.plusTouchSlop(viewConfiguration.touchSlop)
            offset = moved
            onDragChange(DragState(placed.pile, placed.index, moved))
        },
        onDragEnd = {
            onDragChange(null)
            offset?.let(onDrop)
        },
        onDragCancel = { onDragChange(null) },
    )
}

/**
 * Each column's face-down cards as one bar, as tall as they are many, growing and shrinking with the cards, with
 * their count in the middle; and the count of cards left in the stock, on top of it. TalkBack already reads both
 * counts from the cards, so the labels are left out of it.
 */
@Composable
private fun FaceDownPiles(
    state: GameState,
    layout: BoardLayout,
    cardWidth: Dp,
    highlighted: Set<CardIdentity>,
    deal: Deal,
) {
    val density = LocalDensity.current
    val color = SoloThemeExtras.cardColors.back
    // The card's corners in absolute size: CardDimensions.shape is a percentage of the shorter side, the bar's height.
    val shape = RoundedCornerShape(cardWidth * CardDimensions.CORNER_PERCENT / 100)
    val count: @Composable (Int) -> Unit = { value ->
        Text(
            text = value.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = COUNT_SIZE,
            style = LocalTextStyle.current.copy(
                lineHeight = COUNT_SIZE,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            modifier = Modifier
                .clearAndSetSemantics {}
                // Centered on the digits' ink, not their line box, which is taller than a thin bar and carries empty
                // space below the digits: free to overflow, it is placed so the ink's middle is the box's middle.
                .layout { measurable, constraints ->
                    val text = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                    val inkCenter = text[FirstBaseline] - COUNT_SIZE.toPx() * DIGIT_HEIGHT / 2
                    val height = text.height.coerceAtMost(constraints.maxHeight)
                    layout(text.width, height) { text.place(0, (height / 2f - inkCenter).roundToInt()) }
                },
        )
    }
    state.tableau.forEachIndexed { column, pile ->
        key(column) {
            // Dealing, a column has no bar until its turn: it shrinks away, then grows from nothing.
            val target = if (deal.hasBar(column)) with(density) { layout.hiddenBarHeight(pile).toDp() } else 0.dp
            val height by animateDpAsState(target, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "hidden")
            val hidden = pile.count { !it.isFaceUp }
            if (height > 0.dp) {
                val slot = layout.slot(PileRef.Tableau(column))
                Box(
                    Modifier
                        .offset { IntOffset(slot.x.roundToInt(), slot.y.roundToInt()) }
                        .size(cardWidth, height)
                        .background(color, shape),
                    contentAlignment = Alignment.Center,
                ) { if (hidden > 0) count(hidden) }
            }
        }
    }
    // Dealing, the stock is still in the deck.
    if (deal.active) return
    StockCover(state, layout, cardWidth, highlighted)
    if (state.stock.isNotEmpty()) {
        val slot = layout.slot(PileRef.Stock)
        val cardHeight = with(density) { layout.cardHeight.toDp() }
        Box(
            Modifier
                .offset { IntOffset(slot.x.roundToInt(), slot.y.roundToInt()) }
                .size(cardWidth, cardHeight)
                .zIndex(STOCK_COUNT_Z),
            contentAlignment = Alignment.Center,
        ) { count(state.stock.size) }
    }
}

@Composable
private fun EmptySlots(state: GameState, layout: BoardLayout, cardWidth: Dp, onIntent: (GameIntent) -> Unit) {
    val density = LocalDensity.current

    @Composable
    fun Slot(pile: PileRef, description: String, isEmpty: Boolean, icon: ImageVector? = null, borderWidth: Dp? = null) {
        val position = layout.slot(pile)
        val offset = with(density) { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
        val touches = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .offset { offset }
                .width(cardWidth)
                .semantics {
                    contentDescription = description
                    traversalIndex = traversalOrder(pile, 0, layout.handedness)
                    // A slot under cards is covered, so TalkBack reads the top card instead.
                    if (!isEmpty) hideFromAccessibility()
                }
                .clickable(touches, indication = null) { onIntent(GameIntent.Tap(pile, 0)) },
        ) {
            EmptyPileSlot(Modifier.width(cardWidth), icon = icon, borderWidth = borderWidth)
            // Its touch ripple follows the slot's rounded corners, over the outline instead of clipping it.
            Box(Modifier.matchParentSize().clip(CardDimensions.shape).indication(touches, ripple()))
        }
    }
    Slot(
        PileRef.Stock,
        stringResource(if (state.waste.isEmpty()) R.string.stock_empty else R.string.stock_recycle),
        isEmpty = state.stock.isEmpty(),
        icon = Icons.Rounded.Refresh,
    )
    state.foundations.indices.forEach {
        Slot(
            PileRef.Foundation(it),
            stringResource(R.string.foundation_empty),
            isEmpty = state.foundations[it].isEmpty(),
            borderWidth = 1.dp,
        )
    }
    state.tableau.indices.forEach { column ->
        if (state.tableau[column].isEmpty()) {
            Slot(
                PileRef.Tableau(column),
                stringResource(R.string.column_empty, column + 1),
                isEmpty = true,
            )
        }
    }
}

/**
 * The first drag amount leaves out the touch slop the finger travelled before the drag began. Adding it
 * back (along the same direction) keeps the card under the finger, so it lands where it is dropped.
 */
private fun Offset.plusTouchSlop(touchSlop: Float): Offset {
    val distance = getDistance()
    return if (distance == 0f) this else this + this / distance * touchSlop
}

/**
 * For each pile, the lowest and highest cards still on their way to their slots ([moving]). A card flies above the
 * board while it, or any card under it in its pile, is moving: a pile keeps its order when its top card lands before
 * the cards under it. And a card turned face down waits, still drawn, for the cards coming to cover it.
 */
private fun stillMoving(placed: Collection<PlacedCard>, moving: Map<CardIdentity, Boolean>): Map<PileRef, IntRange> =
    placed.filter { moving[it.card.identity()] == true }
        .groupBy { it.pile }
        .mapValues { (_, cards) -> cards.minOf { it.index }..cards.maxOf { it.index } }

private fun isDraggable(state: GameState, placed: PlacedCard): Boolean = placed.card.isFaceUp &&
    when (placed.pile) {
        PileRef.Stock -> false
        PileRef.Waste -> placed.index == state.waste.lastIndex
        is PileRef.Foundation -> placed.index == state.foundations[placed.pile.index].lastIndex
        is PileRef.Tableau -> true
    }

/** Cards to outline for a hint: the cards the suggested move would pick up. */
internal fun hintedCards(state: GameState, move: Move): Set<CardIdentity> = when (move) {
    Move.Draw -> setOfNotNull(state.stock.lastOrNull()?.identity())
    Move.Recycle -> emptySet()
    is Move.WasteToFoundation -> setOfNotNull(state.waste.lastOrNull()?.identity())
    is Move.WasteToTableau -> setOfNotNull(state.waste.lastOrNull()?.identity())
    is Move.TableauToFoundation -> setOfNotNull(state.tableau[move.from].lastOrNull()?.identity())
    is Move.TableauToTableau -> state.tableau[move.from].takeLast(move.count).map { it.identity() }.toSet()
    is Move.FoundationToTableau -> setOfNotNull(state.foundations[move.from].lastOrNull()?.identity())
}

private val GAP = 4.dp
private val COLUMN_GAP = 8.dp
internal const val LIFTED_Z = 10_000f
private const val LANDED_PX = 2

/** Above the stock's cards, under the foundations'. */
private const val STOCK_COUNT_Z = 199f
private val COUNT_SIZE = 9.sp

/** Roboto's digit height, as a fraction of the font size. */
private const val DIGIT_HEIGHT = 0.71f
private const val DRAG_SCALE = 1.05f
