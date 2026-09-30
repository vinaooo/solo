package io.github.vinaooo.solo.domain.hint

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HintEngineTest {

    private val hints = HintEngine()

    @Test
    fun `moving to a foundation beats everything else`() {
        val state = emptyState()
            .copy(stock = down("QD"), waste = up("6H"))
            .withTableau(0, down("3C") + up("AS"))
            .withTableau(1, up("7C"))

        hints.bestHint(state) shouldBe Move.TableauToFoundation(0, 0)
    }

    @Test
    fun `a foundation move that reveals a card beats one that does not`() {
        val state = emptyState()
            .copy(waste = up("AH"))
            .withTableau(3, down("5C") + up("AD"))

        hints.bestHint(state) shouldBe Move.TableauToFoundation(3, 0)
    }

    @Test
    fun `revealing a face-down card beats waste to tableau`() {
        val state = emptyState()
            .copy(waste = up("6H"))
            .withTableau(0, down("3C") + up("9H"))
            .withTableau(1, up("7C"))
            .withTableau(2, up("10S"))

        hints.bestHint(state) shouldBe Move.TableauToTableau(0, 2, 1)
    }

    @Test
    fun `emptying a column onto another column beats waste to tableau`() {
        val state = emptyState()
            .copy(waste = up("6H"))
            .withTableau(0, up("9H"))
            .withTableau(1, up("7C"))
            .withTableau(2, up("10S"))

        hints.bestHint(state) shouldBe Move.TableauToTableau(0, 2, 1)
    }

    @Test
    fun `waste to tableau beats drawing`() {
        val state = emptyState().copy(stock = down("QD"), waste = up("6H")).withTableau(1, up("7C"))

        hints.bestHint(state) shouldBe Move.WasteToTableau(1)
    }

    @Test
    fun `drawing is suggested when nothing better exists, then recycling`() {
        hints.bestHint(emptyState().copy(stock = down("QD"))) shouldBe Move.Draw
        hints.bestHint(emptyState().copy(waste = up("QD"))) shouldBe Move.Recycle
    }

    @Test
    fun `pointless shuffles are never suggested`() {
        val state = emptyState()
            .withTableau(0, up("10D", "9S", "8H"))
            .withTableau(1, up("9C"))
            .withTableau(2, up("KH"))
            .withFoundation(0, suitRun('D', upTo = 5))

        val all = hints.rankedHints(state)
        all shouldNotContain Move.TableauToTableau(0, 1, 1) // 8H onto 9C: face-up card below, reveals nothing
        all shouldNotContain Move.FoundationToTableau(0, 1) // pulling back from foundations
        all.none { it is Move.TableauToTableau && it.from == 2 } shouldBe true // king already at the bottom
    }

    @Test
    fun `no hint when there is nothing useful to do`() {
        hints.bestHint(emptyState().withTableau(0, up("KH"))).shouldBeNull()
    }

    @Test
    fun `only one foundation target is suggested per card`() {
        val state = emptyState().copy(waste = up("AH"))

        hints.rankedHints(state) shouldBe listOf(Move.WasteToFoundation(0), Move.Recycle)
    }
}
