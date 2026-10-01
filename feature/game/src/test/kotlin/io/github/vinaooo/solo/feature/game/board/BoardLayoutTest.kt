package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BoardLayoutTest {

    private val portrait = BoardLayout(width = 1080f, height = 1800f, gap = 12f)
    private val landscape = BoardLayout(width = 2200f, height = 900f, gap = 12f)
    private val dealt = Dealer().deal(SeededShuffler(3), DrawMode.ONE)

    @Test
    fun `in portrait seven columns fill the width`() {
        (portrait.cardWidth * 7 + portrait.gap * 8).toDouble() shouldBe (1080.0 plusOrMinus 0.5)
        portrait.cardHeight shouldBe portrait.cardWidth / CardDimensions.ASPECT_RATIO
    }

    @Test
    fun `in landscape cards shrink to fit the height and the board is centered`() {
        landscape.cardWidth * 7 + landscape.gap * 8 shouldBeLessThanOrEqual 2200f
        (landscape.cardHeight * 3.2f) shouldBeLessThanOrEqual 900f
        landscape.slot(PileRef.Tableau(0)).x shouldBe (2200f - landscape.boardWidth) / 2 + landscape.gap
    }

    @Test
    fun `right-handed, the top row has the four foundations, a gap, then waste and stock`() {
        val y = portrait.gap
        (0 until 4).forEach { portrait.slot(PileRef.Foundation(it)) shouldBe Position(portrait.columnX(it), y) }
        portrait.slot(PileRef.Waste) shouldBe Position(portrait.columnX(5), y)
        portrait.slot(PileRef.Stock) shouldBe Position(portrait.columnX(6), y)
    }

    @Test
    fun `left-handed, the top row has stock, waste, a gap, then the four foundations`() {
        val left = BoardLayout(width = 1080f, height = 1800f, gap = 12f, handedness = Handedness.LEFT)
        val y = left.gap
        left.slot(PileRef.Stock) shouldBe Position(left.columnX(0), y)
        left.slot(PileRef.Waste) shouldBe Position(left.columnX(1), y)
        (0 until 4).forEach { left.slot(PileRef.Foundation(it)) shouldBe Position(left.columnX(3 + it), y) }
    }

    @Test
    fun `columns sit a column gap apart, inside a plain gap margin`() {
        val wide = BoardLayout(width = 1080f, height = 1800f, gap = 12f, columnGap = 24f)
        (wide.cardWidth * 7 + 12f * 2 + 24f * 6).toDouble() shouldBe (1080.0 plusOrMinus 0.5)
        wide.columnX(0) shouldBe 12f
        (wide.columnX(1) - wide.columnX(0)).toDouble() shouldBe ((wide.cardWidth + 24f).toDouble() plusOrMinus 0.01)
    }

    @Test
    fun `three gaps separate the top row from the tableau`() {
        portrait.slot(PileRef.Tableau(0)).y shouldBe portrait.gap + portrait.cardHeight + portrait.gap * 3
    }

    @Test
    fun `a covered column card shows a strip as tall as the step to the next card, the top card none`() {
        val placed = portrait.positions(dealt).values.filter { it.pile == PileRef.Tableau(6) }.sortedBy { it.index }
        val cover = portrait.cover(dealt, placed[0])!!
        cover.edge shouldBe CardCover.Edge.TOP
        cover.strip.toDouble() shouldBe (portrait.faceDownStep.toDouble() plusOrMinus 0.01)
        portrait.cover(dealt, placed.last()).shouldBeNull()
        val stock = portrait.positions(dealt).values.first { it.pile == PileRef.Stock }
        portrait.cover(dealt, stock).shouldBeNull()
    }

    @Test
    fun `every card of a deal gets a position`() {
        portrait.positions(dealt).size shouldBe 52
    }

    @Test
    fun `tableau cards fan downward, face-up cards further apart than face-down ones`() {
        val column = dealt.tableau[6]
        val placed = portrait.positions(dealt)
        val ys = column.map { placed.getValue(it.identity()).position.y }
        val downStep = ys[1] - ys[0]
        val upStep = ys[6] - ys[5]
        downStep.toDouble() shouldBe (portrait.faceDownStep.toDouble() plusOrMinus 0.01)
        upStep.toDouble() shouldBe (downStep.toDouble() plusOrMinus 0.01)
        val withRun = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = List(7) {
                if (it ==
                    0
                ) {
                    listOf(Card(Suit.SPADES, Rank.KING, true), Card(Suit.HEARTS, Rank.QUEEN, true))
                } else {
                    emptyList()
                }
            },
            drawMode = DrawMode.ONE,
        )
        val run = portrait.positions(withRun)
        val step = run.getValue(Card(Suit.HEARTS, Rank.QUEEN).identity()).position.y -
            run.getValue(Card(Suit.SPADES, Rank.KING).identity()).position.y
        step.toDouble() shouldBe (portrait.faceUpStep.toDouble() plusOrMinus 0.01)
    }

    @Test
    fun `long columns compress so they never leave the board`() {
        val tall = List(19) { i -> Card(Suit.entries[i % 4], Rank.entries[i % 13], isFaceUp = i >= 6) }
        val state = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = List(7) { if (it == 0) tall else emptyList() },
            drawMode = DrawMode.ONE,
        )

        val last = landscape.positions(state).values.maxOf { it.position.y }

        (last + landscape.cardHeight) shouldBeLessThanOrEqual landscape.height
    }

    @Test
    fun `draw three fans the top three waste cards`() {
        val waste = listOf(
            Card(Suit.CLUBS, Rank.TWO, true),
            Card(Suit.CLUBS, Rank.THREE, true),
            Card(Suit.CLUBS, Rank.FOUR, true),
            Card(Suit.CLUBS, Rank.FIVE, true),
        )
        val state = GameState(
            stock = emptyList(),
            waste = waste,
            foundations = List(4) { emptyList() },
            tableau = List(7) { emptyList() },
            drawMode = DrawMode.THREE,
        )
        val placed = portrait.positions(state)
        val xs = waste.map { placed.getValue(it.identity()).position.x }

        xs[0] shouldBe xs[1]
        (xs[2] > xs[1]) shouldBe true
        (xs[3] > xs[2]) shouldBe true
        // Right-handed, the fan grows leftward into the empty column and the top card sits on the waste slot.
        xs[3] shouldBe portrait.slot(PileRef.Waste).x
        (xs[1] > portrait.columnX(4)) shouldBe true
        // Left-handed, it grows rightward from the waste slot into the empty column.
        val left = BoardLayout(width = 1080f, height = 1800f, gap = 12f, handedness = Handedness.LEFT)
        val leftXs = waste.map { left.positions(state).getValue(it.identity()).position.x }
        leftXs[1] shouldBe left.slot(PileRef.Waste).x
        (leftXs[3] < left.columnX(2)) shouldBe true
        // The two cards under the top one show their left side, as wide as the fan step; the one below them none.
        listOf(portrait, left).forEach { layout ->
            val placed = layout.positions(state)
            val step = layout.cover(state, placed.getValue(waste[2].identity()))!!
            step.edge shouldBe CardCover.Edge.LEFT
            step.strip.toDouble() shouldBe ((layout.cardWidth * 0.4f).toDouble() plusOrMinus 0.01)
            val under = layout.cover(state, placed.getValue(waste[1].identity()))!!.strip.toDouble()
            under shouldBe (step.strip.toDouble() plusOrMinus 0.01)
            layout.cover(state, placed.getValue(waste[0].identity())).shouldBeNull()
            layout.cover(state, placed.getValue(waste[3].identity())).shouldBeNull()
        }
    }

    @Test
    fun `hit testing finds the pile under a point`() {
        val slot = portrait.slot(PileRef.Tableau(3))
        portrait.pileAt(slot.x + 5, slot.y + portrait.cardHeight * 2, dealt) shouldBe PileRef.Tableau(3)
        val foundation = portrait.slot(PileRef.Foundation(1))
        portrait.pileAt(foundation.x + 5, foundation.y + 5, dealt) shouldBe PileRef.Foundation(1)
        portrait.pileAt(portrait.columnX(4) + 5, portrait.gap + 5, dealt).shouldBeNull()
    }

    @Test
    fun `a card dropped over another column lands there, measured from its center`() {
        val top = portrait.positions(dealt).values.single { it.pile == PileRef.Tableau(0) }
        val dx = portrait.columnX(4) - portrait.columnX(0)

        portrait.dropTarget(dealt, top, dx, 0f) shouldBe PileRef.Tableau(4)
        // Grabbed near its edge, the card still counts where its center is.
        portrait.dropTarget(dealt, top, dx - portrait.cardWidth * 0.4f, 0f) shouldBe PileRef.Tableau(4)
        portrait.dropTarget(dealt, top, dx - portrait.cardWidth * 0.8f, 0f) shouldBe PileRef.Tableau(3)
    }

    @Test
    fun `a card dropped back on its own pile or on nothing goes nowhere`() {
        val top = portrait.positions(dealt).values.single { it.pile == PileRef.Tableau(0) }

        portrait.dropTarget(dealt, top, 3f, 3f).shouldBeNull()
        portrait.dropTarget(dealt, top, portrait.columnX(4) - top.position.x, -top.position.y).shouldBeNull()
    }
}
