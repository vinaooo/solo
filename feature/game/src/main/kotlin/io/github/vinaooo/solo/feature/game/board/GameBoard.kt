package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.EmptyPileSlot
import io.github.vinaooo.solo.core.designsystem.component.PlayingCard
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.feature.game.CardSpot
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.R
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
        val liftedFrom = liftedFrom(placedCards, moving)

        EmptySlots(state, layout, cardWidth, onIntent)

        placedCards.forEach { placed ->
            key(placed.card.identity()) {
                val dragOffset = drag?.takeIf { it.pile == placed.pile && placed.index >= it.index }?.offset
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
                    dragOffset = dragOffset,
                    lifted = placed.index >= (liftedFrom[placed.pile] ?: Int.MAX_VALUE),
                    onMovingChange = { moving[placed.card.identity()] = it },
                    description = accessibility.description,
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
    dragOffset: Offset?,
    lifted: Boolean,
    onMovingChange: (Boolean) -> Unit,
    description: String?,
    gestures: Modifier,
) {
    val target = IntOffset(placed.position.x.roundToInt(), placed.position.y.roundToInt())
    val animated = remember { Animatable(target, IntOffset.VectorConverter) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    // Moving until the spring settles, not when it first reaches the slot: springs overshoot and come back.
    LaunchedEffect(target) {
        onMovingChange(true)
        animated.animateTo(target, spec)
        onMovingChange(false)
    }
    val dragging = dragOffset != null
    PlayingCard(
        card = placed.card,
        highlighted = highlighted,
        contentDescription = description,
        cover = cover,
        modifier = Modifier
            .offset { dragOffset?.let { target + IntOffset(it.x.roundToInt(), it.y.roundToInt()) } ?: animated.value }
            // A card on its way to a new pile flies above every other card, like a dragged one.
            .zIndex(if (dragging || lifted) LIFTED_Z + placed.z else placed.z)
            .scale(if (dragging) DRAG_SCALE else 1f)
            .width(cardWidth)
            .testTag("card_${placed.card.suit}_${placed.card.rank}")
            .then(gestures),
    )
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

@Composable
private fun EmptySlots(state: GameState, layout: BoardLayout, cardWidth: Dp, onIntent: (GameIntent) -> Unit) {
    val density = LocalDensity.current

    @Composable
    fun Slot(pile: PileRef, description: String, isEmpty: Boolean, icon: ImageVector? = null, borderWidth: Dp? = null) {
        val position = layout.slot(pile)
        val offset = with(density) { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
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
                .clickable { onIntent(GameIntent.Tap(pile, 0)) },
        ) {
            EmptyPileSlot(Modifier.width(cardWidth), icon = icon, borderWidth = borderWidth)
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
 * For each pile, the lowest card still on its way to its slot. A card flies above the board while it, or any card
 * under it in its pile, is [moving]: a pile keeps its order when its top card lands before the cards under it.
 */
private fun liftedFrom(placed: Collection<PlacedCard>, moving: Map<CardIdentity, Boolean>): Map<PileRef, Int> =
    placed.filter { moving[it.card.identity()] == true }
        .groupBy { it.pile }
        .mapValues { (_, cards) -> cards.minOf { it.index } }

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
private const val LIFTED_Z = 10_000f
private const val DRAG_SCALE = 1.05f
