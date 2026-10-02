package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
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
 * Pure geometry of the table, in pixels. Cards fill the width in portrait and shrink to fit the height in
 * landscape; tableau columns compress when they would run off the board. The stock and waste sit on the
 * [handedness] side of the top row, the foundations on the other.
 */
class BoardLayout(
    val width: Float,
    val height: Float,
    val gap: Float,
    val handedness: Handedness = Handedness.RIGHT,
    /** Space between neighbouring columns (and top-row piles); [gap] is the margin and the vertical spacing. */
    val columnGap: Float = gap,
) {

    val cardWidth: Float = minOf(
        (width - gap * 2 - columnGap * (COLUMNS - 1)) / COLUMNS,
        (height - gap * (TOP_ROW_GAPS + 2)) / HEIGHT_IN_CARDS * CardDimensions.ASPECT_RATIO,
    )
    val cardHeight: Float = cardWidth / CardDimensions.ASPECT_RATIO
    val boardWidth: Float = cardWidth * COLUMNS + gap * 2 + columnGap * (COLUMNS - 1)
    val faceDownStep: Float = cardHeight * FACE_DOWN_STEP
    val faceUpStep: Float = cardHeight * FACE_UP_STEP

    private val left = (width - boardWidth) / 2
    private val topRowY = gap
    private val tableauY = topRowY + cardHeight + gap * TOP_ROW_GAPS

    // Wide enough for the shrunk rank and suit of the cards under the top one, into the empty column beside the waste.
    private val wasteFanStep = cardWidth * WASTE_FAN_STEP
    private val rightHanded = handedness == Handedness.RIGHT

    // With the board's 4dp gap: each bar is 0.88dp short of its cards' steps, and 2.5dp above its face-up cards.
    private val hiddenBarShortfall = gap * HIDDEN_BAR_SHORTFALL
    private val hiddenBarGap = gap * HIDDEN_BAR_GAP

    // Every column's face-up cards, a bar or not, start this much below its steps, so they keep to one staircase.
    private val faceUpLift = hiddenBarGap - hiddenBarShortfall

    fun columnX(column: Int): Float = left + gap + column * (cardWidth + columnGap)

    fun slot(pile: PileRef): Position = when (pile) {
        PileRef.Stock -> Position(columnX(if (rightHanded) COLUMNS - 1 else 0), topRowY)
        PileRef.Waste -> Position(columnX(if (rightHanded) COLUMNS - 2 else 1), topRowY)
        is PileRef.Foundation ->
            Position(columnX(pile.index + if (rightHanded) 0 else FIRST_FOUNDATION_COLUMN), topRowY)
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
            when {
                dx > 0 -> Cover(CardCover.Edge.LEFT, dx)
                dx < 0 -> Cover(CardCover.Edge.RIGHT, -dx)
                state.drawMode == DrawMode.THREE -> Cover(CardCover.Edge.LEFT, wasteFanStep)
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
        val shift = if (rightHanded) (state.waste.lastIndex - firstFanned).coerceAtLeast(0) else 0
        return state.waste.indices.map { i ->
            val fanIndex = (i - firstFanned).coerceAtLeast(0)
            Position(base.x + (fanIndex - shift) * wasteFanStep, base.y)
        }
    }

    /**
     * Height of the bar that stands in for a column's face-down cards; 0 when it has none. Each face-down card
     * takes a [faceDownStep], and the bar is its cards' steps less a fixed shortfall. The face-up cards start a
     * gap below it, and every column, a bar or not, starts its face-up cards that same lift below its steps.
     */
    fun hiddenBarHeight(column: List<Card>): Float {
        val steps = column.count { !it.isFaceUp } * faceDownStep
        // The face-down cards sit where the first face-up card is: below the bar and its gap, squeezed alike.
        return if (steps ==
            0f
        ) {
            0f
        } else {
            columnOffsets(column).first() * (steps - hiddenBarShortfall) / (steps + faceUpLift)
        }
    }

    private fun columnOffsets(pile: List<Card>): List<Float> {
        val hidden = pile.count { !it.isFaceUp }
        val bar = hidden * faceDownStep + faceUpLift
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
        const val FIRST_FOUNDATION_COLUMN = 3

        /** Gaps between the top row and the tableau. */
        const val TOP_ROW_GAPS = 3

        /** Top row + a tableau of at least 2.2 card heights must fit in landscape. */
        const val HEIGHT_IN_CARDS = 3.2f
        const val FACE_DOWN_STEP = 0.12f
        const val HIDDEN_BAR_SHORTFALL = 0.22f
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
