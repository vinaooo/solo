package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
import io.github.vinaooo.solo.domain.model.BoardAlignment
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit

data class Position(val x: Float, val y: Float)

/** A card's identity regardless of its face, so it keeps its composable (and animates) when it moves. */
data class CardIdentity(val suit: Suit, val rank: Rank)

fun Card.identity() = CardIdentity(suit, rank)

/** A card's [CardCover] in pixels. */
data class Cover(val edge: CardCover.Edge, val strip: Float)

data class PlacedCard(val card: Card, val pile: PileRef, val index: Int, val position: Position, val z: Float)

/**
 * Pure geometry of the table, in pixels; tableau columns compress when they would run off the board.
 *
 * Classic (portrait): cards fill the width, and the stock and waste sit on the [handedness] side of a top row, the
 * foundations on the other. At the [alignment] bottom, the board sits as low as it can with room for the tallest
 * column a game can have: a bar of six face-down cards and a run from king to ace.
 *
 * [sideways] (landscape): the piles stand in two columns beside the tableau, which gets the whole height. On the
 * [handedness] edge, the stock with the waste under it (its draw-three fan growing down); next to it, the four
 * foundations one under the other, which set the card size.
 */
@Suppress("LongParameterList") // The board's size and spacing, and the three settings that place its piles.
class BoardLayout(
    val width: Float,
    val height: Float,
    val gap: Float,
    val handedness: Handedness = Handedness.RIGHT,
    /** Space between neighbouring columns (and top-row piles); [gap] is the margin and the vertical spacing. */
    val columnGap: Float = gap,
    val alignment: BoardAlignment = BoardAlignment.TOP,
    val sideways: Boolean = false,
) {

    /** Between the piles and the tableau: under the top row, or beside the foundations (72dp with the 4dp gap). */
    private val groupGap = gap * if (sideways) SIDEWAYS_GAPS else TOP_ROW_GAPS

    val cardWidth: Float = if (sideways) {
        minOf(
            (width - gap * 2 - columnGap * COLUMNS - groupGap) / (COLUMNS + SIDE_COLUMNS),
            (height - gap * 2 - columnGap * (FOUNDATIONS - 1)) / FOUNDATIONS * CardDimensions.ASPECT_RATIO,
        )
    } else {
        minOf(
            (width - gap * 2 - columnGap * (COLUMNS - 1)) / COLUMNS,
            (height - gap * (TOP_ROW_GAPS + 2)) / HEIGHT_IN_CARDS * CardDimensions.ASPECT_RATIO,
        )
    }
    val cardHeight: Float = cardWidth / CardDimensions.ASPECT_RATIO
    val boardWidth: Float = cardWidth * COLUMNS + gap * 2 + columnGap * (COLUMNS - 1) +
        if (sideways) (cardWidth + columnGap) * SIDE_COLUMNS - columnGap + groupGap else 0f
    val faceDownStep: Float = cardHeight * FACE_DOWN_STEP
    val faceUpStep: Float = cardHeight * FACE_UP_STEP

    // With the board's 4dp gap: each bar is 0.12dp taller than its cards' steps, plus 0.524dp for every card past
    // the first, and 2.5dp above its face-up cards.
    private val hiddenBarExtra = gap * HIDDEN_BAR_EXTRA
    private val hiddenBarSpread = gap * HIDDEN_BAR_SPREAD
    private val hiddenBarGap = gap * HIDDEN_BAR_GAP

    private val left = (width - boardWidth) / 2
    private val topRowY = gap + when {
        sideways || alignment == BoardAlignment.TOP -> 0f
        else -> {
            // As columnOffsets lays it out, unsqueezed.
            val bar = MAX_HIDDEN * faceDownStep + hiddenBarExtra + (MAX_HIDDEN - 1) * hiddenBarSpread + hiddenBarGap
            val tallestColumn = bar + (Rank.entries.size - 1) * faceUpStep + cardHeight
            val boardHeight = gap + cardHeight + gap * TOP_ROW_GAPS + tallestColumn + gap
            (height - boardHeight).coerceAtLeast(0f)
        }
    }
    private val tableauY = if (sideways) topRowY else topRowY + cardHeight + gap * TOP_ROW_GAPS

    // Wide enough for the shrunk rank and suit of the cards under the top one, into the empty column beside the waste;
    // sideways, tall enough for them, down the stock's column.
    private val wasteFanStep = (if (sideways) cardHeight else cardWidth) * WASTE_FAN_STEP
    private val rightHanded = handedness == Handedness.RIGHT

    // Sideways and left-handed, the stock's and the foundations' columns come first.
    private val tableauX = left + gap +
        if (sideways && !rightHanded) (cardWidth + columnGap) * SIDE_COLUMNS - columnGap + groupGap else 0f
    private val foundationsX = if (rightHanded) {
        tableauX + (cardWidth + columnGap) * COLUMNS - columnGap + groupGap
    } else {
        left + gap + cardWidth + columnGap
    }
    private val stockX = if (rightHanded) foundationsX + cardWidth + columnGap else left + gap

    fun columnX(column: Int): Float = tableauX + column * (cardWidth + columnGap)

    fun slot(pile: PileRef): Position = when (pile) {
        PileRef.Stock -> Position(if (sideways) stockX else columnX(if (rightHanded) COLUMNS - 1 else 0), topRowY)
        PileRef.Waste -> if (sideways) {
            Position(stockX, topRowY + cardHeight + columnGap)
        } else {
            Position(columnX(if (rightHanded) COLUMNS - 2 else 1), topRowY)
        }
        is PileRef.Foundation -> if (sideways) {
            Position(foundationsX, topRowY + pile.index * (cardHeight + columnGap))
        } else {
            Position(columnX(pile.index + if (rightHanded) 0 else FIRST_FOUNDATION_COLUMN), topRowY)
        }
        is PileRef.Tableau -> Position(columnX(pile.index), tableauY)
    }

    fun positions(state: GameState): Map<CardIdentity, PlacedCard> = buildMap {
        fun place(card: Card, pile: PileRef, index: Int, position: Position, z: Float) {
            put(card.identity(), PlacedCard(card, pile, index, position, z))
        }
        state.stock.forEachIndexed { i, card -> place(card, PileRef.Stock, i, slot(PileRef.Stock), STOCK_Z + i) }
        wastePositions(state).forEachIndexed { i, position ->
            place(state.waste[i], PileRef.Waste, i, position, WASTE_Z + i)
        }
        state.foundations.forEachIndexed { f, pile ->
            pile.forEachIndexed { i, card ->
                place(
                    card,
                    PileRef.Foundation(f),
                    i,
                    slot(PileRef.Foundation(f)),
                    FOUNDATION_Z + i,
                )
            }
        }
        state.tableau.forEachIndexed { column, pile ->
            columnOffsets(pile).forEachIndexed { i, dy ->
                val base = slot(PileRef.Tableau(column))
                place(pile[i], PileRef.Tableau(column), i, Position(base.x, base.y + dy), TABLEAU_Z + i)
            }
        }
    }

    /**
     * What shows of a card past the next card of its pile: a strip along the top in a column, along a side in the
     * waste fan. Null when nothing covers it, or when the next card hides it completely, except under a draw-three
     * fan: those cards were fanned a moment ago and keep their face there, so it doesn't spring back to the center
     * while the new cards fly over it.
     */
    fun cover(state: GameState, placed: PlacedCard): Cover? = when (val pile = placed.pile) {
        is PileRef.Tableau -> state.tableau[pile.index].takeIf { placed.index < it.lastIndex }?.let { column ->
            val offsets = columnOffsets(column)
            Cover(CardCover.Edge.TOP, offsets[placed.index + 1] - offsets[placed.index])
        }
        PileRef.Waste -> if (placed.index < state.waste.lastIndex) {
            val positions = wastePositions(state)
            val dx = positions[placed.index + 1].x - positions[placed.index].x
            val dy = positions[placed.index + 1].y - positions[placed.index].y
            when {
                dy > 0 -> Cover(CardCover.Edge.TOP, dy)
                dx > 0 -> Cover(CardCover.Edge.LEFT, dx)
                dx < 0 -> Cover(CardCover.Edge.RIGHT, -dx)
                state.drawMode == DrawMode.THREE ->
                    Cover(if (sideways) CardCover.Edge.TOP else CardCover.Edge.LEFT, wasteFanStep)
                else -> null
            }
        } else {
            null
        }
        else -> null
    }

    /** The pile a dragged card is dropped on. Tableau columns accept drops anywhere along their length. */
    fun pileAt(x: Float, y: Float, state: GameState): PileRef? {
        val topRow = listOf(PileRef.Stock, PileRef.Waste) + state.foundations.indices.map { PileRef.Foundation(it) }
        topRow.firstOrNull { contains(slot(it), x, y, cardHeight) }?.let { return it }
        return state.tableau.indices
            .map { PileRef.Tableau(it) }
            .firstOrNull { contains(slot(it), x, y, height - tableauY) }
    }

    /**
     * Where [placed] lands after being dragged by ([dx], [dy]): the pile under the card's center,
     * or null when that is nowhere or the pile it came from.
     */
    fun dropTarget(state: GameState, placed: PlacedCard, dx: Float, dy: Float): PileRef? {
        val x = placed.position.x + dx + cardWidth / 2
        val y = placed.position.y + dy + cardHeight / 2
        return pileAt(x, y, state)?.takeUnless { it == placed.pile }
    }

    private fun contains(slot: Position, x: Float, y: Float, extent: Float) =
        x in slot.x..(slot.x + cardWidth) && y in slot.y..(slot.y + extent)

    private fun wastePositions(state: GameState): List<Position> {
        val base = slot(PileRef.Waste)
        val fanned = if (state.drawMode == DrawMode.THREE) VISIBLE_WASTE_CARDS else 1
        val firstFanned = (state.waste.size - fanned).coerceAtLeast(0)
        // Right-handed, the fan grows leftward so the top card stays next to the stock.
        val shift = if (rightHanded && !sideways) (state.waste.lastIndex - firstFanned).coerceAtLeast(0) else 0
        return state.waste.indices.map { i ->
            val step = ((i - firstFanned).coerceAtLeast(0) - shift) * wasteFanStep
            if (sideways) Position(base.x, base.y + step) else Position(base.x + step, base.y)
        }
    }

    /**
     * Height of the bar that stands in for a column's face-down cards; 0 when it has none. Each face-down card
     * takes a [faceDownStep], and the bar is its cards' steps, a fixed extra, and a spread for every card past
     * the first. The face-up cards start a gap below it; in a column without a bar they start at the top, level
     * with the bars.
     */
    fun hiddenBarHeight(column: List<Card>): Float {
        val hidden = column.count { !it.isFaceUp }
        if (hidden == 0) return 0f
        val bar = hidden * faceDownStep + hiddenBarExtra + (hidden - 1) * hiddenBarSpread
        // The face-down cards sit where the first face-up card is: below the bar and its gap, squeezed alike.
        return columnOffsets(column).first() * bar / (bar + hiddenBarGap)
    }

    private fun columnOffsets(pile: List<Card>): List<Float> {
        val hidden = pile.count { !it.isFaceUp }
        val bar = if (hidden > 0) {
            hidden * faceDownStep + hiddenBarExtra + (hidden - 1) * hiddenBarSpread + hiddenBarGap
        } else {
            0f
        }
        // Squeezed when the column would run off the board.
        val total = bar + (pile.size - hidden - 1).coerceAtLeast(0) * faceUpStep
        val available = height - tableauY - gap - cardHeight
        val scale = if (total > available) available / total else 1f
        // Face-down cards wait, unseen, where the first face-up card is: one that turns over slides up with the bar.
        val faceUp = List(pile.size - hidden) { (bar + it * faceUpStep) * scale }
        return List(hidden) { bar * scale } + faceUp
    }

    private companion object {
        const val COLUMNS = 7

        /** Face-down cards in the last column of the deal, the most any column can have. */
        const val MAX_HIDDEN = 6
        const val FIRST_FOUNDATION_COLUMN = 3
        const val FOUNDATIONS = 4

        /** Gaps between the foundations and the tableau, sideways. */
        const val SIDEWAYS_GAPS = 18

        /** Sideways: the stock and waste's column and the foundations'. */
        const val SIDE_COLUMNS = 2

        /** Gaps between the top row and the tableau. */
        const val TOP_ROW_GAPS = 3

        /** Top row + a tableau of at least 2.2 card heights must fit in landscape. */
        const val HEIGHT_IN_CARDS = 3.2f
        const val FACE_DOWN_STEP = 0.12f
        const val HIDDEN_BAR_EXTRA = 0.03f
        const val HIDDEN_BAR_SPREAD = 0.131f
        const val HIDDEN_BAR_GAP = 0.625f
        const val FACE_UP_STEP = 0.28f
        const val WASTE_FAN_STEP = 0.4f
        const val VISIBLE_WASTE_CARDS = 3
        const val WASTE_Z = 100f

        /** Above the waste, so a card going back to the stock can pass between the two. */
        const val STOCK_Z = 150f
        const val FOUNDATION_Z = 200f
        const val TABLEAU_Z = 300f
    }
}
