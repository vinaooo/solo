package io.github.vinaooo.solo.domain.autocomplete

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AutoCompleterTest {

    private val autoCompleter = AutoCompleter()

    private val allFaceUp = emptyState()
        .withFoundation(0, suitRun('C', upTo = 10))
        .withFoundation(1, suitRun('D', upTo = 10))
        .withFoundation(2, suitRun('H', upTo = 10))
        .withFoundation(3, suitRun('S', upTo = 10))
        .withTableau(0, up("KC", "QD", "JC"))
        .withTableau(1, up("KD", "QC", "JD"))
        .withTableau(2, up("KH", "QS", "JH"))
        .withTableau(3, up("KS", "QH", "JS"))

    @Test
    fun `available once stock and waste are empty and every card is face up`() {
        autoCompleter.canAutoComplete(allFaceUp).shouldBeTrue()
    }

    @Test
    fun `not available with cards in the stock, the waste, or face down`() {
        autoCompleter.canAutoComplete(allFaceUp.copy(stock = down("2C"))).shouldBeFalse()
        autoCompleter.canAutoComplete(allFaceUp.copy(waste = up("2C"))).shouldBeFalse()
        autoCompleter.canAutoComplete(allFaceUp.withTableau(4, down("2C") + up("KS"))).shouldBeFalse()
    }

    @Test
    fun `not available once the game is won`() {
        val won = emptyState()
            .withFoundation(0, suitRun('C'))
            .withFoundation(1, suitRun('D'))
            .withFoundation(2, suitRun('H'))
            .withFoundation(3, suitRun('S'))
        autoCompleter.canAutoComplete(won).shouldBeFalse()
        autoCompleter.nextMove(won).shouldBeNull()
    }

    @Test
    fun `next move sends the lowest available card to its foundation`() {
        autoCompleter.nextMove(allFaceUp) shouldBe Move.TableauToFoundation(0, 0)
    }

    @Test
    fun `lowest card wins even when it is not in the first column`() {
        val state = emptyState()
            .withFoundation(0, suitRun('C', upTo = 11))
            .withFoundation(1, suitRun('D', upTo = 9))
            .withFoundation(2, suitRun('H', upTo = 12))
            .withFoundation(3, suitRun('S', upTo = 12))
            .withTableau(0, up("KC", "QC"))
            .withTableau(1, up("KD", "QD", "JC"))
            .withTableau(5, up("KS"))
            .withTableau(6, up("KH", "10D"))

        autoCompleter.nextMove(state) shouldBe Move.TableauToFoundation(6, 1)
    }

    @Test
    fun `playing the next move repeatedly wins the game`() {
        val engine = GameEngine()
        var state = allFaceUp
        var guard = 0
        while (!state.isWon && guard++ < 52) {
            val move = autoCompleter.nextMove(state)!!
            state = (engine.apply(state, move) as MoveOutcome.Applied).state
        }
        state.isWon.shouldBeTrue()
    }
}
