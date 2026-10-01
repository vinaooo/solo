package io.github.vinaooo.solo.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import io.github.vinaooo.solo.core.designsystem.R
import io.github.vinaooo.solo.core.designsystem.theme.CardColors
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.model.SuitColor
import kotlin.math.roundToInt

/**
 * A card; [contentDescription] replaces what TalkBack says for it, which is otherwise its name. When another card
 * covers all but the top [coveredStrip] of it, the rank and suit shrink into a row centered in that strip.
 */
@Composable
fun PlayingCard(
    card: Card,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentDescription: String? = null,
    coveredStrip: Dp? = null,
) {
    val colors = SoloThemeExtras.cardColors
    val description =
        contentDescription ?: if (card.isFaceUp) cardName(card) else stringResource(R.string.card_face_down)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(CardDimensions.ASPECT_RATIO)
            .clip(CardDimensions.shape)
            .background(if (card.isFaceUp) colors.face else colors.back)
            .border(
                width = if (highlighted) 3.dp else 1.dp,
                color = if (highlighted) colors.highlight else colors.border,
                shape = CardDimensions.shape,
            )
            .clearAndSetSemantics { this.contentDescription = description },
    ) {
        if (card.isFaceUp) CardFace(card, colors, maxWidth, coveredStrip) else CardBack(colors)
    }
}

@Composable
private fun CardFace(card: Card, colors: CardColors, width: Dp, coveredStrip: Dp?) {
    val ink = if (card.suit.color == SuitColor.RED) colors.redSuits else colors.blackSuits
    val density = LocalDensity.current
    val size = with(density) { (width * CENTER_SUIT_RATIO).toSp() }
    val overlap = with(density) { (width * FACE_SPACING_RATIO).roundToPx() }
    // The same spring that moves cards, so the face changes while the card that covers or uncovers it travels.
    val progress by animateFloatAsState(
        if (coveredStrip != null) 1f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "face",
    )
    // Once uncovered, the row keeps its strip while it slides back to the center.
    val lastStrip = remember { mutableStateOf(0.dp) }
    SideEffect { if (coveredStrip != null) lastStrip.value = coveredStrip }
    // Uncovered: the suit under the rank, centered on the card. Covered: the suit beside the rank, in the strip.
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Text(text = card.rank.symbol, color = ink, fontSize = size, fontWeight = FontWeight.Bold, lineHeight = size)
            Text(text = card.suit.symbol, color = ink, fontSize = size, lineHeight = size)
        },
    ) { measurables, constraints ->
        val (rank, suit) = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        // The glyphs carry blank space above and below them, so the stacked suit overlaps the rank's line a little.
        val stackedTop = (constraints.maxHeight - (rank.height + suit.height - overlap)) / 2
        // Covered, the pair also shrinks, so the row is laid out from the scaled sizes.
        val scale = lerp(1f, COVERED_FACE_SCALE, progress)
        val rankWidth = (rank.width * scale).roundToInt()
        val rowLeft = (width - rankWidth - (suit.width * scale).roundToInt()) / 2
        val strip = (coveredStrip ?: lastStrip.value).roundToPx()
        // Center the letters' ink, not their line box, which carries empty descender space below them.
        val inkCenter = (rank[FirstBaseline] - size.toPx() * CAP_HEIGHT / 2) * scale
        val rowTop = (strip / 2 - inkCenter).roundToInt()
        val rowRank = IntOffset(rowLeft, rowTop)
        val rowSuit = IntOffset(rowLeft + rankWidth, rowTop + ((rank.height - suit.height) * scale / 2).roundToInt())
        val scaled: GraphicsLayerScope.() -> Unit = {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
        layout(width, constraints.maxHeight) {
            rank.placeWithLayer(
                lerp(IntOffset((width - rank.width) / 2, stackedTop), rowRank, progress),
                layerBlock = scaled,
            )
            suit.placeWithLayer(
                lerp(IntOffset((width - suit.width) / 2, stackedTop + rank.height - overlap), rowSuit, progress),
                layerBlock = scaled,
            )
        }
    }
}

@Composable
private fun CardBack(colors: CardColors) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = size.width * BACK_INSET_RATIO
        val inner = Size(size.width - inset * 2, size.height - inset * 2)
        val radius = CornerRadius(size.width * BACK_CORNER_RATIO)
        drawRoundRect(
            color = colors.backPattern.copy(alpha = 0.35f),
            topLeft = Offset(inset, inset),
            size = inner,
            cornerRadius = radius,
        )
        val step = size.width / DOTS_PER_ROW
        var y = inset + step / 2
        while (y < size.height - inset) {
            var x = inset + step / 2
            while (x < size.width - inset) {
                drawCircle(
                    color = colors.backPattern.copy(alpha = 0.55f),
                    radius = step * DOT_RATIO,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
        }
    }
}

/** Suit glyph forced to text presentation (U+FE0E), so devices don't draw it as an emoji that ignores the color. */
val Suit.symbol: String
    get() = when (this) {
        Suit.CLUBS -> "\u2663"
        Suit.DIAMONDS -> "\u2666"
        Suit.HEARTS -> "\u2665"
        Suit.SPADES -> "\u2660"
    } + TEXT_PRESENTATION

private const val TEXT_PRESENTATION = "\uFE0E"

val Rank.symbol: String
    get() = when (this) {
        Rank.ACE -> "A"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
        else -> value.toString()
    }

/** The spoken name of a card, such as "Queen of Hearts", whichever way it faces. */
@Composable
fun cardName(card: Card): String =
    stringResource(R.string.card_description, stringResource(card.rank.nameRes), stringResource(card.suit.nameRes))

@get:StringRes
val Suit.nameRes: Int
    get() = when (this) {
        Suit.CLUBS -> R.string.suit_clubs
        Suit.DIAMONDS -> R.string.suit_diamonds
        Suit.HEARTS -> R.string.suit_hearts
        Suit.SPADES -> R.string.suit_spades
    }

@get:StringRes
val Rank.nameRes: Int
    get() = when (this) {
        Rank.ACE -> R.string.rank_ace
        Rank.TWO -> R.string.rank_2
        Rank.THREE -> R.string.rank_3
        Rank.FOUR -> R.string.rank_4
        Rank.FIVE -> R.string.rank_5
        Rank.SIX -> R.string.rank_6
        Rank.SEVEN -> R.string.rank_7
        Rank.EIGHT -> R.string.rank_8
        Rank.NINE -> R.string.rank_9
        Rank.TEN -> R.string.rank_10
        Rank.JACK -> R.string.rank_jack
        Rank.QUEEN -> R.string.rank_queen
        Rank.KING -> R.string.rank_king
    }

private const val CENTER_SUIT_RATIO = 0.4f
private const val FACE_SPACING_RATIO = 0.08f
private const val COVERED_FACE_SCALE = 0.65f

/** Roboto's capital and digit height, as a fraction of the font size. */
private const val CAP_HEIGHT = 0.71f
private const val BACK_INSET_RATIO = 0.08f
private const val BACK_CORNER_RATIO = 0.06f
private const val DOTS_PER_ROW = 7f
private const val DOT_RATIO = 0.18f
