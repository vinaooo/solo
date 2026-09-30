package io.github.vinaooo.solo.domain.interaction

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MoveResolverTest {

    private val resolver = MoveResolver()

    @Nested
    inner class Tap {
        @Test
        fun `tapping the stock draws, or recycles when it is empty`() {
            resolver.resolveTap(emptyState().copy(stock = down("2C")), PileRef.Stock, 0) shouldBe Move.Draw
            resolver.resolveTap(emptyState().copy(waste = up("2C")), PileRef.Stock, 0) shouldBe Move.Recycle
            resolver.resolveTap(emptyState(), PileRef.Stock, 0).shouldBeNull()
        }

        @Test
        fun `waste card prefers a foundation over the tableau`() {
            val state = emptyState().copy(waste = up("AH")).withTableau(0, up("2S"))

            resolver.resolveTap(state, PileRef.Waste, 0) shouldBe Move.WasteToFoundation(0)
        }

        @Test
        fun `waste card goes to the first matching column when no foundation fits`() {
            val state = emptyState().copy(waste = up("6H")).withTableau(2, up("7C")).withTableau(4, up("7S"))

            resolver.resolveTap(state, PileRef.Waste, 0) shouldBe Move.WasteToTableau(2)
        }

        @Test
        fun `a card goes to the foundation holding its suit`() {
            val state = emptyState()
                .copy(waste = up("2H"))
                .withFoundation(0, suitRun('S', upTo = 1))
                .withFoundation(3, suitRun('H', upTo = 1))

            resolver.resolveTap(state, PileRef.Waste, 0) shouldBe Move.WasteToFoundation(3)
        }

        @Test
        fun `top tableau card prefers the foundation`() {
            val state = emptyState().withTableau(1, down("9C") + up("AD")).withTableau(2, up("2S"))

            resolver.resolveTap(state, PileRef.Tableau(1), 1) shouldBe Move.TableauToFoundation(1, 0)
        }

        @Test
        fun `tapping inside a column moves the whole run from that card`() {
            val state = emptyState()
                .withTableau(0, down("KD") + up("9H", "8S"))
                .withTableau(5, up("10S"))
                .withTableau(6, up("10C"))

            resolver.resolveTap(state, PileRef.Tableau(0), 1) shouldBe Move.TableauToTableau(0, 5, 2)
        }

        @Test
        fun `a run is never sent to a foundation, even if its base card would fit`() {
            val state = emptyState()
                .withFoundation(2, suitRun('C', upTo = 2))
                .withTableau(0, up("3C", "2H"))
                .withTableau(4, up("4D"))

            resolver.resolveTap(state, PileRef.Tableau(0), 0) shouldBe Move.TableauToTableau(0, 4, 2)
        }

        @Test
        fun `a king goes to the first empty column`() {
            val state = emptyState().copy(waste = up("KH")).withTableau(0, up("5C")).withTableau(1, up("9D"))

            resolver.resolveTap(state, PileRef.Waste, 0) shouldBe Move.WasteToTableau(2)
        }

        @Test
        fun `a king already at the bottom of a column is not moved to another empty column`() {
            val state = emptyState().withTableau(0, up("KH", "QS"))

            resolver.resolveTap(state, PileRef.Tableau(0), 0).shouldBeNull()
        }

        @Test
        fun `foundation card goes back to a matching column`() {
            val state = emptyState().withFoundation(2, suitRun('H', upTo = 6)).withTableau(3, up("7C"))

            resolver.resolveTap(state, PileRef.Foundation(2), 5) shouldBe Move.FoundationToTableau(2, 3)
        }

        @Test
        fun `tapping a face-down card or an invalid index does nothing`() {
            val state = emptyState().withTableau(0, down("KD") + up("9H"))

            resolver.resolveTap(state, PileRef.Tableau(0), 0).shouldBeNull()
            resolver.resolveTap(state, PileRef.Tableau(0), 5).shouldBeNull()
            resolver.resolveTap(state, PileRef.Waste, 0).shouldBeNull()
        }
    }

    @Nested
    inner class Drop {
        @Test
        fun `dropping a run on a column builds the tableau move`() {
            val state = emptyState().withTableau(0, down("KD") + up("9H", "8S")).withTableau(1, up("10C"))

            resolver.resolveDrop(state, PileRef.Tableau(0), 1, PileRef.Tableau(1)) shouldBe
                Move.TableauToTableau(0, 1, 2)
        }

        @Test
        fun `dropping on foundations and from waste or foundation`() {
            val state = emptyState()
                .copy(waste = up("AH"))
                .withTableau(0, up("AS"))
                .withFoundation(1, suitRun('D', upTo = 2))
                .withTableau(2, up("3S"))

            resolver.resolveDrop(state, PileRef.Waste, 0, PileRef.Foundation(3)) shouldBe Move.WasteToFoundation(3)
            resolver.resolveDrop(state, PileRef.Tableau(0), 0, PileRef.Foundation(0)) shouldBe
                Move.TableauToFoundation(0, 0)
            resolver.resolveDrop(state, PileRef.Foundation(1), 1, PileRef.Tableau(2)) shouldBe
                Move.FoundationToTableau(1, 2)
        }

        @Test
        fun `illegal drops resolve to nothing`() {
            val state = emptyState().copy(waste = up("5H")).withTableau(0, up("9S", "8H"))

            resolver.resolveDrop(state, PileRef.Waste, 0, PileRef.Tableau(0)).shouldBeNull()
            resolver.resolveDrop(state, PileRef.Tableau(0), 0, PileRef.Foundation(0)).shouldBeNull()
            resolver.resolveDrop(state, PileRef.Waste, 0, PileRef.Stock).shouldBeNull()
            resolver.resolveDrop(state, PileRef.Waste, 0, PileRef.Waste).shouldBeNull()
            resolver.resolveDrop(state, PileRef.Stock, 0, PileRef.Tableau(1)).shouldBeNull()
        }
    }
}
