package io.github.vinaooo.solo.feature.game.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import io.github.vinaooo.solo.core.designsystem.component.cardName
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.R

/** What TalkBack says for a card, and the semantics and tap that go with it. */
/** [modifier] handles taps; the card draws their ripple from [touches], so it follows its rounded corners. */
internal class CardAccessibility(val description: String?, val modifier: Modifier, val touches: InteractionSource)

@Composable
internal fun cardAccessibility(
    role: CardRole,
    placed: PlacedCard,
    hinted: Boolean,
    destinations: List<PileRef>,
    layout: BoardLayout,
    onIntent: (GameIntent) -> Unit,
): CardAccessibility {
    val hintLabel = stringResource(R.string.a11y_hinted)
    val actions = moveActions(placed, destinations, onIntent)
    val touches = remember { MutableInteractionSource() }
    val modifier = Modifier
        .semantics {
            traversalIndex = traversalOrder(placed.pile, placed.index, layout.handedness, layout.sideways)
            if (role == CardRole.Hidden) hideFromAccessibility()
            if (hinted) stateDescription = hintLabel
            if (actions.isNotEmpty()) customActions = actions
        }
        .clickable(
            interactionSource = touches,
            indication = null,
            onClickLabel = stringResource(
                if (placed.pile == PileRef.Stock) R.string.a11y_click_draw else R.string.a11y_click_move,
            ),
        ) { onIntent(GameIntent.Tap(placed.pile, placed.index)) }
    return CardAccessibility(roleDescription(role), modifier, touches)
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
