package io.github.vinaooo.solo.core.designsystem.component

import android.graphics.BlurMaskFilter
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
 * leaves only a [cover] strip of it showing, the rank and suit shrink into that strip: side by side along the top,
 * or still stacked along a side. A card lying on another one casts a [shadow] past that edge, so the edge shows
 * where the cards have no outline.
 */
@Composable
fun PlayingCard(
    card: Card,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentDescription: String? = null,
    cover: CardCover? = null,
    shadow: CardCover.Edge? = null,
) {
    val colors = SoloThemeExtras.cardColors
    val description =
        contentDescription ?: if (card.isFaceUp) cardName(card) else stringResource(R.string.card_face_down)
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(CardDimensions.ASPECT_RATIO)
            .drawBehind { shadow?.let { drawEdgeShadow(it, SHADOW_WIDTH.toPx()) } }
            .clip(CardDimensions.shape)
            .background(if (card.isFaceUp) colors.face else colors.back)
            // No outline of its own: only a hinted card is outlined.
            .then(if (highlighted) Modifier.border(3.dp, colors.highlight, CardDimensions.shape) else Modifier)
            .clearAndSetSemantics { this.contentDescription = description },
    ) {
        // A face-down card is its back color alone, from the background above.
        if (card.isFaceUp) CardFace(card, colors, maxWidth, cover)
    }
}

@Composable
private fun CardFace(card: Card, colors: CardColors, width: Dp, cover: CardCover?) {
    val ink = if (card.suit.color == SuitColor.RED) colors.redSuits else colors.blackSuits
    val density = LocalDensity.current
    val size = with(density) { (width * CENTER_SUIT_RATIO).toSp() }
    // The same spring that moves cards, so the face changes while the card that covers or uncovers it travels.
    val progress by animateFloatAsState(
        if (cover != null) 1f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "face",
    )
    // Once uncovered, the face keeps its strip while it slides back to the center.
    val lastCover = remember { mutableStateOf(CardCover(CardCover.Edge.TOP, 0.dp)) }
    SideEffect { if (cover != null) lastCover.value = cover }
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Text(text = card.rank.symbol, color = ink, fontSize = size, fontWeight = FontWeight.Bold, lineHeight = size)
            Text(text = card.suit.symbol, color = ink, fontSize = size, lineHeight = size)
        },
    ) { measurables, constraints ->
        val (rank, suit) = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val face = FaceGeometry(
            rank = rank,
            suit = suit,
            card = IntSize(constraints.maxWidth, constraints.maxHeight),
            overlap = (width * FACE_SPACING_RATIO).roundToPx(),
            scale = lerp(1f, COVERED_FACE_SCALE, progress),
            // Center the letters' ink, not their line box, which carries empty descender space below them.
            inkCenter = rank[FirstBaseline] - size.toPx() * CAP_HEIGHT / 2,
        )
        val shown = cover ?: lastCover.value
        val (coveredRank, coveredSuit) = if (shown.edge == CardCover.Edge.TOP) {
            face.row(shown.strip.roundToPx(), (width * COVERED_GAP_RATIO * progress).roundToPx())
        } else {
            face.stackedIn(shown.strip.roundToPx(), shown.edge)
        }
        val scaled: GraphicsLayerScope.() -> Unit = {
            scaleX = face.scale
            scaleY = face.scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            rank.placeWithLayer(lerp(face.centeredRank, coveredRank, progress), layerBlock = scaled)
            suit.placeWithLayer(lerp(face.centeredSuit, coveredSuit, progress), layerBlock = scaled)
        }
    }
}

/**
 * Where the rank and suit go. Uncovered: the suit under the rank, centered on the card. Covered, at [scale]: the suit
 * beside the rank in a strip along the top, or the suit under the rank in a strip along a side.
 */
private class FaceGeometry(
    val rank: Placeable,
    val suit: Placeable,
    val card: IntSize,
    /** The glyphs carry blank space above and below them, so the stacked suit overlaps the rank's line a little. */
    val overlap: Int,
    val scale: Float,
    val inkCenter: Float,
) {
    private val stackedTop = (card.height - (rank.height + suit.height - overlap)) / 2
    val centeredRank = IntOffset((card.width - rank.width) / 2, stackedTop)
    val centeredSuit = IntOffset((card.width - suit.width) / 2, stackedTop + rank.height - overlap)

    fun row(strip: Int, gap: Int): Pair<IntOffset, IntOffset> {
        val rankWidth = (rank.width * scale).roundToInt()
        val left = (card.width - rankWidth - gap - (suit.width * scale).roundToInt()) / 2
        val top = (strip / 2 - inkCenter * scale).roundToInt()
        return IntOffset(left, top) to
            IntOffset(left + rankWidth + gap, top + ((rank.height - suit.height) * scale / 2).roundToInt())
    }

    fun stackedIn(strip: Int, edge: CardCover.Edge): Pair<IntOffset, IntOffset> {
        val centerX = if (edge == CardCover.Edge.RIGHT) card.width - strip / 2f else strip / 2f
        val top = (card.height - (rank.height + suit.height - overlap) * scale) / 2
        return IntOffset((centerX - rank.width * scale / 2).roundToInt(), top.roundToInt()) to
            IntOffset(
                (centerX - suit.width * scale / 2).roundToInt(),
                (top + (rank.height - overlap) * scale).roundToInt(),
            )
    }
}

/**
 * A soft shadow past the card's [edge], onto the card underneath: the card's own rounded shape, blurred and nudged
 * toward that edge. It is kept to that side and within the card's span, where the card underneath is, so it follows
 * the rounded corners without spilling onto the table.
 */
private fun DrawScope.drawEdgeShadow(edge: CardCover.Edge, blur: Float) {
    val corner = size.width * CardDimensions.CORNER_PERCENT / PERCENT
    val shift = blur / 2
    val (dx, dy) = when (edge) {
        CardCover.Edge.TOP -> 0f to -shift
        CardCover.Edge.LEFT -> -shift to 0f
        CardCover.Edge.RIGHT -> shift to 0f
    }
    val margin = blur * 2
    val side = when (edge) {
        CardCover.Edge.TOP -> Rect(0f, -margin, size.width, corner)
        CardCover.Edge.LEFT -> Rect(-margin, 0f, corner, size.height)
        CardCover.Edge.RIGHT -> Rect(size.width - corner, 0f, size.width + margin, size.height)
    }
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Black.copy(alpha = SHADOW_ALPHA).toArgb()
        maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
    }
    clipRect(side.left, side.top, side.right, side.bottom) {
        drawIntoCanvas {
            it.nativeCanvas.drawRoundRect(dx, dy, size.width + dx, size.height + dy, corner, corner, paint)
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
private val SHADOW_WIDTH = 5.dp
private const val SHADOW_ALPHA = 0.35f
private const val PERCENT = 100
private const val COVERED_GAP_RATIO = 0.04f

/** Roboto's capital and digit height, as a fraction of the font size. */
private const val CAP_HEIGHT = 0.71f
