package io.github.vinaooo.solo.feature.game.board

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

data class PlacedCard(val card: Card, val pile: PileRef, val index: Int, val position: Position, val z: Float)

/**
 * Pure geometry of the table, in pixels. Cards fill the width in portrait and shrink to fit the height in
 * landscape; tableau columns compress when they would run off the board. The stock and waste sit on the
 * [handedness] side of the top row, the foundations on the other.
 */
class BoardLayout(val width: Float, val height: Float, val gap: Float, val handedness: Handedness = Handedness.RIGHT) {

    val cardWidth: Float = minOf(
        (width - gap * (COLUMNS + 1)) / COLUMNS,
        (height - gap * 3) / HEIGHT_IN_CARDS * CardDimensions.ASPECT_RATIO,
    )
    val cardHeight: Float = cardWidth / CardDimensions.ASPECT_RATIO
    val boardWidth: Float = cardWidth * COLUMNS + gap * (COLUMNS + 1)
    val faceDownStep: Float = cardHeight * FACE_DOWN_STEP
    val faceUpStep: Float = cardHeight * FACE_UP_STEP

    private val left = (width - boardWidth) / 2
    private val topRowY = gap
    private val tableauY = topRowY + cardHeight + gap
    private val wasteFanStep = cardWidth * WASTE_FAN_STEP
    private val rightHanded = handedness == Handedness.RIGHT

    fun columnX(column: Int): Float = left + gap + column * (cardWidth + gap)

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
        state.stock.forEachIndexed { i, card -> place(card, PileRef.Stock, i, slot(PileRef.Stock), i.toFloat()) }
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

    private fun columnOffsets(pile: List<Card>): List<Float> {
        if (pile.isEmpty()) return emptyList()
        val steps = pile.dropLast(1).map { if (it.isFaceUp) faceUpStep else faceDownStep }
        val available = height - tableauY - gap - cardHeight
        val scale = steps.sum().let { total -> if (total > available) available / total else 1f }
        return steps.runningFold(0f) { y, step -> y + step * scale }
    }

    private companion object {
        const val COLUMNS = 7
        const val FIRST_FOUNDATION_COLUMN = 3

        /** Top row + a tableau of at least 2.2 card heights must fit in landscape. */
        const val HEIGHT_IN_CARDS = 3.2f
        const val FACE_DOWN_STEP = 0.12f
        const val FACE_UP_STEP = 0.28f
        const val WASTE_FAN_STEP = 0.3f
        const val VISIBLE_WASTE_CARDS = 3
        const val WASTE_Z = 100f
        const val FOUNDATION_Z = 200f
        const val TABLEAU_Z = 300f
    }
}
