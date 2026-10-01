package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.animateIntOffsetAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaooo.solo.core.designsystem.component.EmptyPileSlot
import io.github.vinaooo.solo.core.designsystem.component.PlayingCard
import io.github.vinaooo.solo.core.designsystem.component.cardName
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
        val layout = remember(constraints.maxWidth, constraints.maxHeight, density, handedness) {
            BoardLayout(
                constraints.maxWidth.toFloat(),
                constraints.maxHeight.toFloat(),
                with(density) { GAP.toPx() },
                handedness,
            )
        }
        val cardWidth = with(density) { layout.cardWidth.toDp() }
        val highlighted = remember(state, hint) { hint?.let { hintedCards(state, it) }.orEmpty() }
        var drag by remember { mutableStateOf<DragState?>(null) }
        val currentState by rememberUpdatedState(state)
        val currentOnIntent by rememberUpdatedState(onIntent)

        EmptySlots(state, layout, cardWidth, onIntent)

        layout.positions(state).values.forEach { placed ->
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
                    coveredStrip = layout.coveredStrip(state, placed)?.let { with(density) { it.toDp() } },
                    dragOffset = dragOffset,
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

/** What TalkBack says for a card, and the semantics and tap that go with it. */
private class CardAccessibility(val description: String?, val modifier: Modifier)

@Composable
private fun cardAccessibility(
    role: CardRole,
    placed: PlacedCard,
    hinted: Boolean,
    destinations: List<PileRef>,
    handedness: Handedness,
    onIntent: (GameIntent) -> Unit,
): CardAccessibility {
    val hintLabel = stringResource(R.string.a11y_hinted)
    val actions = moveActions(placed, destinations, onIntent)
    val modifier = Modifier
        .semantics {
            traversalIndex = traversalOrder(placed.pile, placed.index, handedness)
            if (role == CardRole.Hidden) hideFromAccessibility()
            if (hinted) stateDescription = hintLabel
            if (actions.isNotEmpty()) customActions = actions
        }
        .clickable(
            onClickLabel = stringResource(
                if (placed.pile == PileRef.Stock) R.string.a11y_click_draw else R.string.a11y_click_move,
            ),
        ) { onIntent(GameIntent.Tap(placed.pile, placed.index)) }
    return CardAccessibility(roleDescription(role), modifier)
}

@Composable
private fun roleDescription(role: CardRole): String? = when (role) {
    CardRole.Hidden -> null
    is CardRole.StockTop -> pluralStringResource(R.plurals.a11y_stock, role.count, role.count)
    is CardRole.WasteTop -> stringResource(R.string.a11y_card_in_waste, cardName(role.card))
    is CardRole.FoundationTop -> stringResource(R.string.a11y_card_in_foundation, cardName(role.card))
    is CardRole.InColumn -> if (role.faceDownBelow > 0) {
        pluralStringResource(
            R.plurals.a11y_card_in_column_above_hidden,
            role.faceDownBelow,
            cardName(role.card),
            role.column + 1,
            role.faceDownBelow,
        )
    } else {
        stringResource(R.string.a11y_card_in_column, cardName(role.card), role.column + 1)
    }
}

/**
 * TalkBack actions that move the card to each pile it can go to, so a player can pick the destination without
 * dragging. One foundation action is enough: a card fits a single foundation, or any empty one for an ace.
 */
@Composable
private fun moveActions(
    placed: PlacedCard,
    destinations: List<PileRef>,
    onIntent: (GameIntent) -> Unit,
): List<CustomAccessibilityAction> {
    val targets = destinations.filterIsInstance<PileRef.Foundation>().take(1) +
        destinations.filterIsInstance<PileRef.Tableau>()
    return targets.map { to ->
        val label = if (to is PileRef.Tableau) {
            stringResource(R.string.a11y_move_to_column, to.index + 1)
        } else {
            stringResource(R.string.a11y_move_to_foundation)
        }
        CustomAccessibilityAction(label) {
            onIntent(GameIntent.Drop(placed.pile, placed.index, to))
            true
        }
    }
}

/** One card, animated to its slot, or following the finger while [dragOffset] is set. */
@Composable
private fun BoardCard(
    placed: PlacedCard,
    cardWidth: Dp,
    highlighted: Boolean,
    coveredStrip: Dp?,
    dragOffset: Offset?,
    description: String?,
    gestures: Modifier,
) {
    val target = IntOffset(placed.position.x.roundToInt(), placed.position.y.roundToInt())
    val animated by animateIntOffsetAsState(target, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "card")
    val dragging = dragOffset != null
    // A card on its way to a new pile flies above every other card, like a dragged one; stacks keep their order.
    val lifted = dragging || animated != target
    PlayingCard(
        card = placed.card,
        highlighted = highlighted,
        contentDescription = description,
        coveredStrip = coveredStrip,
        modifier = Modifier
            .offset { dragOffset?.let { target + IntOffset(it.x.roundToInt(), it.y.roundToInt()) } ?: animated }
            .zIndex(if (lifted) LIFTED_Z + placed.z else placed.z)
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
private const val LIFTED_Z = 10_000f
private const val DRAG_SCALE = 1.05f
