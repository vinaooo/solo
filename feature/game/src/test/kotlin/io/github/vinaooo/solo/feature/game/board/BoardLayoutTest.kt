package io.github.vinaooo.solo.feature.game.board

import io.github.vinaooo.solo.core.designsystem.component.CardCover
import io.github.vinaooo.solo.core.designsystem.component.CardDimensions
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
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
        val run = listOf(Card(Suit.SPADES, Rank.KING, true), Card(Suit.HEARTS, Rank.QUEEN, true))
        val state = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = List(7) { if (it == 0) run else emptyList() },
            drawMode = DrawMode.ONE,
        )
        val placed = portrait.positions(state).values.filter { it.pile == PileRef.Tableau(0) }.sortedBy { it.index }
        val cover = portrait.cover(state, placed[0])!!
        cover.edge shouldBe CardCover.Edge.TOP
        cover.strip.toDouble() shouldBe (portrait.faceUpStep.toDouble() plusOrMinus 0.01)
        portrait.cover(state, placed.last()).shouldBeNull()
        val stock = portrait.positions(dealt).values.first { it.pile == PileRef.Stock }
        portrait.cover(dealt, stock).shouldBeNull()
    }

    @Test
    fun `every card of a deal gets a position`() {
        portrait.positions(dealt).size shouldBe 52
    }

    @Test
    fun `face-down cards make a bar a step per card tall, and the face-up card starts a gap below it`() {
        val column = dealt.tableau[6]
        val placed = portrait.positions(dealt)
        val ys = column.map { placed.getValue(it.identity()).position.y }
        val bar = portrait.hiddenBarHeight(column)
        // Six steps, the bar's extra and five spreads, then the gap before the face-up card.
        val expected = portrait.faceDownStep * 6 + portrait.gap * 0.03f + portrait.gap * 0.131f * 5
        bar.toDouble() shouldBe (expected.toDouble() plusOrMinus 0.01)
        val top = portrait.slot(PileRef.Tableau(6)).y
        (ys[6] - top).toDouble() shouldBe ((bar + portrait.gap * 0.625f).toDouble() plusOrMinus 0.01)
        // The unseen face-down cards wait under the face-up one, so the next to turn over slides up from there.
        ys.take(6).forEach { it shouldBe ys[6] }
        portrait.hiddenBarHeight(dealt.tableau[0]) shouldBe 0f
        portrait.hiddenBarHeight(dealt.tableau[1]).toDouble() shouldBe
            (portrait.singleBarHeight.toDouble() plusOrMinus 0.01)
        // Each first face-up card is a step per face-down card down, lifted when there is a bar; level with it if not.
        val firsts = dealt.tableau.mapIndexed { column, pile ->
            placed.getValue(pile.last().identity()).position.y - portrait.slot(PileRef.Tableau(column)).y
        }
        firsts.forEachIndexed { column, y ->
            val step = portrait.faceDownStep
            val lift = if (column > 0) portrait.gap * (0.625f + 0.03f + 0.131f * (column - 1)) else 0f
            y.toDouble() shouldBe ((column * step + lift).toDouble() plusOrMinus 0.01)
        }
    }

    @Test
    fun `face-up cards in a column fan downward a step apart`() {
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
        landscape.hiddenBarHeight(tall) shouldBeLessThanOrEqual landscape.faceDownStep * 6
    }

    @Test
    fun `at the bottom, the tallest possible column ends a gap above the bottom edge`() {
        val low = BoardLayout(width = 1080f, height = 1800f, gap = 12f, alignment = BoardAlignment.BOTTOM)
        val tallest = List(6) { Card(Suit.CLUBS, Rank.entries[it], isFaceUp = false) } +
            Rank.entries.reversed().map { Card(Suit.HEARTS, it, isFaceUp = true) }
        val state = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = List(7) { if (it == 6) tallest else emptyList() },
            drawMode = DrawMode.ONE,
        )
        val column = low.positions(state).values.filter { it.pile == PileRef.Tableau(6) }.sortedBy { it.index }

        val bottom = low.height - (column.last().position.y + low.cardHeight)
        bottom.toDouble() shouldBe (low.gap.toDouble() plusOrMinus 0.5)
        low.slot(PileRef.Stock).y shouldBeGreaterThan low.gap
        // Not squeezed: every face-up card a full step below the one before.
        (column.last().position.y - column[6].position.y).toDouble() shouldBe
            ((low.faceUpStep * 12).toDouble() plusOrMinus 0.5)
    }

    @Test
    fun `at the bottom, a board with no height to spare stays at the top`() {
        BoardLayout(width = 2200f, height = 900f, gap = 12f, alignment = BoardAlignment.BOTTOM)
            .slot(PileRef.Stock).y shouldBe 12f
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
        // The two cards under the top one show their left side, as wide as the fan step. The one below them is
        // hidden, but keeps the same face so it doesn't spring back while drawn cards fly over it.
        listOf(portrait, left).forEach { layout ->
            val placed = layout.positions(state)
            val step = layout.cover(state, placed.getValue(waste[2].identity()))!!
            step.edge shouldBe CardCover.Edge.LEFT
            step.strip.toDouble() shouldBe ((layout.cardWidth * 0.4f).toDouble() plusOrMinus 0.01)
            val under = layout.cover(state, placed.getValue(waste[1].identity()))!!.strip.toDouble()
            under shouldBe (step.strip.toDouble() plusOrMinus 0.01)
            val hidden = layout.cover(state, placed.getValue(waste[0].identity()))!!
            hidden.edge shouldBe CardCover.Edge.LEFT
            hidden.strip.toDouble() shouldBe (step.strip.toDouble() plusOrMinus 0.01)
            layout.cover(state, placed.getValue(waste[3].identity())).shouldBeNull()
        }
        // Drawing one, the waste is a plain pile: the cards under the top one keep a centered face.
        val drawOne = state.copy(drawMode = DrawMode.ONE)
        portrait.cover(drawOne, portrait.positions(drawOne).getValue(waste[2].identity())).shouldBeNull()
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

    @Test
    fun `sideways, the four stacked foundations set the card size and nine columns fit the width`() {
        listOf(sideways(Handedness.LEFT), sideways(Handedness.RIGHT), sideways(Handedness.LEFT, width = 1000f))
            .forEach { layout ->
                val bottom = layout.slot(PileRef.Foundation(3)).y + layout.cardHeight
                bottom shouldBeLessThanOrEqual layout.height - layout.gap + 0.01f
                layout.boardWidth shouldBeLessThanOrEqual layout.width + 0.01f
            }
        val tall = sideways(Handedness.LEFT)
        (tall.height - tall.gap - (tall.slot(PileRef.Foundation(3)).y + tall.cardHeight)).toDouble() shouldBe
            (0.0 plusOrMinus 0.5)
    }

    @Test
    fun `sideways and left-handed, stock over waste, the foundations, then the tableau from the top`() {
        val layout = sideways(Handedness.LEFT)
        val left = (layout.width - layout.boardWidth) / 2 + layout.gap
        layout.slot(PileRef.Stock) shouldBe Position(left, layout.gap)
        layout.slot(PileRef.Waste) shouldBe Position(left, layout.gap + layout.cardHeight + 24f)
        val foundationsX = left + layout.cardWidth + 24f
        (0 until 4).forEach {
            layout.slot(PileRef.Foundation(it)) shouldBe
                Position(foundationsX, layout.gap + it * (layout.cardHeight + 24f))
        }
        val tableau = layout.slot(PileRef.Tableau(0))
        tableau.x.toDouble() shouldBe ((foundationsX + layout.cardWidth + 216f).toDouble() plusOrMinus 0.01)
        tableau.y shouldBe layout.gap
    }

    @Test
    fun `sideways and right-handed, the mirror image`() {
        val layout = sideways(Handedness.RIGHT)
        val left = (layout.width - layout.boardWidth) / 2 + layout.gap
        layout.slot(PileRef.Tableau(0)) shouldBe Position(left, layout.gap)
        val foundationsX = layout.columnX(6) + layout.cardWidth + 216f
        (0 until 4).forEach { layout.slot(PileRef.Foundation(it)).x shouldBe foundationsX }
        val stock = layout.slot(PileRef.Stock)
        stock shouldBe Position(foundationsX + layout.cardWidth + 24f, layout.gap)
        (stock.x + layout.cardWidth + layout.gap).toDouble() shouldBe
            ((layout.width - (layout.width - layout.boardWidth) / 2).toDouble() plusOrMinus 0.5)
        layout.slot(PileRef.Waste).x shouldBe stock.x
    }

    @Test
    fun `sideways, the draw three fan grows down on either hand, its cards showing their top`() {
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
        listOf(sideways(Handedness.LEFT), sideways(Handedness.RIGHT)).forEach { layout ->
            val placed = layout.positions(state)
            val positions = waste.map { placed.getValue(it.identity()).position }
            val slot = layout.slot(PileRef.Waste)
            val step = layout.cardHeight * 0.4f
            positions.forEach { it.x shouldBe slot.x }
            positions[0].y shouldBe slot.y
            positions[1].y shouldBe slot.y
            positions[3].y.toDouble() shouldBe ((slot.y + step * 2).toDouble() plusOrMinus 0.01)
            listOf(waste[0], waste[1], waste[2]).forEach {
                val cover = layout.cover(state, placed.getValue(it.identity()))!!
                cover.edge shouldBe CardCover.Edge.TOP
                cover.strip.toDouble() shouldBe (step.toDouble() plusOrMinus 0.01)
            }
            layout.cover(state, placed.getValue(waste[3].identity())).shouldBeNull()
        }
    }

    @Test
    fun `sideways, columns get the whole height and hit testing finds the stacked piles`() {
        val layout = sideways(Handedness.LEFT)
        val tall = List(19) { i -> Card(Suit.entries[i % 4], Rank.entries[i % 13], isFaceUp = i >= 6) }
        val state = GameState(
            stock = emptyList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = List(7) { if (it == 0) tall else emptyList() },
            drawMode = DrawMode.ONE,
        )
        val last = layout.positions(state).values.maxOf { it.position.y }
        (last + layout.cardHeight) shouldBeLessThanOrEqual layout.height
        (last + layout.cardHeight) shouldBeGreaterThan layout.height - layout.gap * 2

        val foundation = layout.slot(PileRef.Foundation(3))
        layout.pileAt(foundation.x + 5, foundation.y + 5, state) shouldBe PileRef.Foundation(3)
        val waste = layout.slot(PileRef.Waste)
        layout.pileAt(waste.x + 5, waste.y + 5, state) shouldBe PileRef.Waste
        val column = layout.slot(PileRef.Tableau(6))
        layout.pileAt(column.x + 5, layout.height - 5, state) shouldBe PileRef.Tableau(6)
    }

    private fun sideways(handedness: Handedness, width: Float = 2200f) =
        BoardLayout(width, height = 900f, gap = 12f, handedness, columnGap = 24f, sideways = true)
}
