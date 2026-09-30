package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.scoring.ScoreEvent
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class KlondikeRulesTest {

    private val rules = KlondikeRules()

    @Nested
    inner class Draw {
        @Test
        fun `draw one moves the top stock card face up to the waste`() {
            val state = emptyState().copy(stock = down("2C", "3C", "4C"))

            val result = rules.perform(state, Move.Draw)

            result.state.stock shouldContainExactly down("2C", "3C")
            result.state.waste shouldContainExactly up("4C")
            result.events.shouldBeEmpty()
        }

        @Test
        fun `draw three moves up to three cards, last drawn on top`() {
            val state = emptyState(DrawMode.THREE).copy(stock = down("2C", "3C", "4C", "5C"))

            val result = rules.perform(state, Move.Draw)

            result.state.stock shouldContainExactly down("2C")
            result.state.waste shouldContainExactly up("5C", "4C", "3C")
        }

        @Test
        fun `draw three with fewer cards draws what is left`() {
            val state = emptyState(DrawMode.THREE).copy(stock = down("2C", "3C"))

            rules.perform(state, Move.Draw).state.waste shouldContainExactly up("3C", "2C")
        }

        @Test
        fun `cannot draw from an empty stock`() {
            rules.isLegal(emptyState(), Move.Draw).shouldBeFalse()
        }
    }

    @Nested
    inner class Recycle {
        @Test
        fun `turns the waste back into the stock face down, preserving draw order`() {
            val state = emptyState().copy(waste = up("4C", "3C", "2C"))

            val result = rules.perform(state, Move.Recycle)

            result.state.stock shouldContainExactly down("2C", "3C", "4C")
            result.state.waste.shouldBeEmpty()
            result.state.recycles shouldBe 1
            result.events shouldContainExactly listOf(ScoreEvent.Recycle(DrawMode.ONE, recycleNumber = 1))
        }

        @Test
        fun `only legal when stock is empty and waste is not`() {
            rules.isLegal(emptyState().copy(waste = up("2C")), Move.Recycle).shouldBeTrue()
            rules.isLegal(emptyState(), Move.Recycle).shouldBeFalse()
            rules.isLegal(emptyState().copy(stock = down("3C"), waste = up("2C")), Move.Recycle).shouldBeFalse()
        }
    }

    @Nested
    inner class WasteToTableau {
        @Test
        fun `moves the top waste card onto a valid column`() {
            val state = emptyState().copy(waste = up("2C", "6H")).withTableau(5, up("7S"))
            rules.isLegal(state, Move.WasteToTableau(to = 5)).shouldBeTrue()

            val result = rules.perform(state, Move.WasteToTableau(to = 5))

            result.state.waste shouldContainExactly up("2C")
            result.state.tableau[5] shouldContainExactly up("7S", "6H")
            result.events shouldContainExactly listOf(ScoreEvent.WasteToTableau)
        }

        @Test
        fun `illegal on a non-matching column or empty waste`() {
            val state = emptyState().copy(waste = up("6H")).withTableau(0, up("7H"))
            rules.isLegal(state, Move.WasteToTableau(to = 0)).shouldBeFalse()
            rules.isLegal(emptyState(), Move.WasteToTableau(to = 0)).shouldBeFalse()
        }
    }

    @Nested
    inner class WasteToFoundation {
        @Test
        fun `moves the top waste card to a matching foundation`() {
            val state = emptyState().copy(waste = up("AH"))
            rules.isLegal(state, Move.WasteToFoundation(to = 2)).shouldBeTrue()

            val result = rules.perform(state, Move.WasteToFoundation(to = 2))

            result.state.foundations[2] shouldContainExactly up("AH")
            result.events shouldContainExactly listOf(ScoreEvent.WasteToFoundation)
        }

        @Test
        fun `illegal when the card does not fit`() {
            rules.isLegal(emptyState().copy(waste = up("2H")), Move.WasteToFoundation(to = 0)).shouldBeFalse()
        }
    }

    @Nested
    inner class TableauToTableau {
        @Test
        fun `moves a run and reveals the new top card`() {
            val state = emptyState()
                .withTableau(0, down("KD") + up("9H", "8S"))
                .withTableau(1, up("10C"))
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 1, count = 2)).shouldBeTrue()

            val result = rules.perform(state, Move.TableauToTableau(from = 0, to = 1, count = 2))

            result.state.tableau[0] shouldContainExactly up("KD")
            result.state.tableau[1] shouldContainExactly up("10C", "9H", "8S")
            result.events shouldContainExactly listOf(ScoreEvent.CardRevealed)
        }

        @Test
        fun `moving within the face-up part earns nothing`() {
            val state = emptyState()
                .withTableau(0, up("JD", "10S", "9H"))
                .withTableau(1, up("10C"))

            val result = rules.perform(state, Move.TableauToTableau(from = 0, to = 1, count = 1))

            result.events.shouldBeEmpty()
        }

        @Test
        fun `king run can move to an empty column`() {
            val state = emptyState().withTableau(0, down("2C") + up("KH", "QS"))

            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 3, count = 2)).shouldBeTrue()
        }

        @Test
        fun `illegal moves`() {
            val state = emptyState()
                .withTableau(0, down("2C") + up("9H", "8S"))
                .withTableau(1, up("10C"))
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 1, count = 3)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 1, count = 1)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 0, count = 1)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = 1, count = 0)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = 2, to = 1, count = 1)).shouldBeFalse()
        }
    }

    @Nested
    inner class TableauToFoundation {
        @Test
        fun `moves the top card and reveals the one below`() {
            val state = emptyState().withTableau(4, down("9D") + up("AS"))
            rules.isLegal(state, Move.TableauToFoundation(from = 4, to = 0)).shouldBeTrue()

            val result = rules.perform(state, Move.TableauToFoundation(from = 4, to = 0))

            result.state.tableau[4] shouldContainExactly up("9D")
            result.state.foundations[0] shouldContainExactly up("AS")
            result.events shouldContainExactly listOf(ScoreEvent.TableauToFoundation, ScoreEvent.CardRevealed)
        }

        @Test
        fun `illegal from an empty column`() {
            rules.isLegal(emptyState(), Move.TableauToFoundation(from = 0, to = 0)).shouldBeFalse()
        }
    }

    @Nested
    inner class FoundationToTableau {
        @Test
        fun `moves the top foundation card back to a matching column`() {
            val state = emptyState().withFoundation(1, suitRun('H', upTo = 6)).withTableau(3, up("7S"))
            rules.isLegal(state, Move.FoundationToTableau(from = 1, to = 3)).shouldBeTrue()

            val result = rules.perform(state, Move.FoundationToTableau(from = 1, to = 3))

            result.state.foundations[1] shouldContainExactly suitRun('H', upTo = 5)
            result.state.tableau[3] shouldContainExactly up("7S", "6H")
            result.events shouldContainExactly listOf(ScoreEvent.FoundationToTableau)
        }

        @Test
        fun `illegal when the card does not fit the column`() {
            val state = emptyState().withFoundation(1, suitRun('H', upTo = 6)).withTableau(0, up("7D"))
            rules.isLegal(state, Move.FoundationToTableau(from = 1, to = 0)).shouldBeFalse()
        }

        @Test
        fun `illegal from an empty foundation`() {
            rules.isLegal(emptyState().withTableau(0, up("7S")), Move.FoundationToTableau(0, 0)).shouldBeFalse()
        }
    }

    @Nested
    inner class OutOfRange {
        private val state = emptyState()
            .copy(waste = up("KH"))
            .withTableau(0, up("KS"))
            .withFoundation(0, up("AC"))

        @ParameterizedTest
        @ValueSource(ints = [-1, 7])
        fun `tableau indexes outside 0 until 7 are illegal`(index: Int) {
            rules.isLegal(state, Move.WasteToTableau(to = index)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = index, to = 1, count = 1)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToTableau(from = 0, to = index, count = 1)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToFoundation(from = index, to = 1)).shouldBeFalse()
            rules.isLegal(state, Move.FoundationToTableau(from = 0, to = index)).shouldBeFalse()
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, 4])
        fun `foundation indexes outside 0 until 4 are illegal`(index: Int) {
            rules.isLegal(state, Move.WasteToFoundation(to = index)).shouldBeFalse()
            rules.isLegal(state, Move.TableauToFoundation(from = 0, to = index)).shouldBeFalse()
            rules.isLegal(state, Move.FoundationToTableau(from = index, to = 1)).shouldBeFalse()
        }

        @Test
        fun `the last valid indexes are accepted`() {
            val edge = emptyState().copy(waste = up("KH")).withFoundation(3, up("AC")).withTableau(6, up("2D"))
            rules.isLegal(edge, Move.WasteToTableau(to = 5)).shouldBeTrue()
            rules.isLegal(edge, Move.FoundationToTableau(from = 3, to = 6)).shouldBeTrue()
            val toFoundation = edge.withTableau(6, up("2C"))
            rules.isLegal(toFoundation, Move.TableauToFoundation(from = 6, to = 3)).shouldBeTrue()
        }
    }

    @Nested
    inner class LegalMoves {
        @Test
        fun `lists every legal move and nothing else`() {
            val state = emptyState()
                .copy(stock = down("QD"), waste = up("AH"))
                .withTableau(0, up("7S"))
                .withTableau(1, up("6D"))

            rules.legalMoves(state).toSet() shouldBe setOf(
                Move.Draw,
                Move.WasteToFoundation(to = 0),
                Move.WasteToFoundation(to = 1),
                Move.WasteToFoundation(to = 2),
                Move.WasteToFoundation(to = 3),
                Move.TableauToTableau(from = 1, to = 0, count = 1),
            )
        }

        @Test
        fun `every listed move is legal`() {
            val state = emptyState().copy(waste = up("KC")).withTableau(0, up("QH"))
            rules.legalMoves(state).forEach { rules.isLegal(state, it).shouldBeTrue() }
        }
    }
}
