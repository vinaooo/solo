package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef
import kotlin.math.roundToInt

/**
 * The stock's cover: a card back over its cards, so a drawn card comes out from under the stock and a returning one
 * slides back under it. It fades out as the last card is drawn and back in when the waste is turned over. It shows
 * the hint's outline when the hint is to draw, as the stock's top card would.
 */
@Composable
internal fun StockCover(state: GameState, layout: BoardLayout, cardWidth: Dp, highlighted: Set<CardIdentity>) {
    val alpha by animateFloatAsState(
        if (state.stock.isEmpty()) 0f else 1f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "stock cover",
    )
    if (alpha == 0f) return
    val colors = SoloThemeExtras.cardColors
    val slot = layout.slot(PileRef.Stock)
    val hinted = state.stock.lastOrNull()?.identity() in highlighted
    Box(
        Modifier
            .offset { IntOffset(slot.x.roundToInt(), slot.y.roundToInt()) }
            // Sized like a card (width and aspect ratio), so it matches the stock's cards pixel for pixel.
            .width(cardWidth)
            .aspectRatio(CardDimensions.ASPECT_RATIO)
            .zIndex(STOCK_COVER_Z)
            .alpha(alpha)
            .background(colors.back, CardDimensions.shape)
            .then(if (hinted) Modifier.border(3.dp, colors.highlight, CardDimensions.shape) else Modifier),
    )
}

/** Over the stock's cards and anything sliding under it, under the count of cards left. */
internal const val STOCK_COVER_Z = 198f
