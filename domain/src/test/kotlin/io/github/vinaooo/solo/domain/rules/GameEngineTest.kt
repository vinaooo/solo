package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class GameEngineTest {

    private val engine = GameEngine()

    private fun GameEngine.applied(state: io.github.vinaooo.solo.domain.model.GameState, move: Move) =
        apply(state, move).shouldBeInstanceOf<MoveOutcome.Applied>().state

    @Test
    fun `illegal moves are rejected and leave the state untouched`() {
        engine.apply(emptyState(), Move.Draw) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `a legal move counts as a move and adds its points`() {
        val state = emptyState().copy(waste = up("6H"), score = 20, moves = 3).withTableau(0, up("7S"))

        val next = engine.applied(state, Move.WasteToTableau(to = 0))

        next.moves shouldBe 4
        next.score shouldBe 25
    }

    @Test
    fun `revealing a card adds its points on top of the move`() {
        val state = emptyState().withTableau(0, down("9D") + up("AS"))

        engine.applied(state, Move.TableauToFoundation(from = 0, to = 0)).score shouldBe 15
    }

    @Test
    fun `score never goes below zero`() {
        val state = emptyState().copy(waste = up("2C"), score = 40)

        engine.applied(state, Move.Recycle).score shouldBe 0
    }

    @Test
    fun `winning move adds the time bonus`() {
        val almostWon = emptyState()
            .copy(elapsedSeconds = 100, score = 500)
            .withFoundation(0, suitRun('C'))
            .withFoundation(1, suitRun('D'))
            .withFoundation(2, suitRun('H'))
            .withFoundation(3, suitRun('S', upTo = 12))
            .withTableau(0, up("KS"))

        val won = engine.applied(almostWon, Move.TableauToFoundation(from = 0, to = 3))

        won.isWon.shouldBeTrue()
        won.score shouldBe 500 + 10 + 7_000
    }

    @Test
    fun `time penalty applies once per completed ten-second period`() {
        val state = emptyState().copy(score = 100, elapsedSeconds = 8)

        val later = engine.tick(state, elapsedSeconds = 31)

        later.elapsedSeconds shouldBe 31
        later.score shouldBe 94
        engine.tick(later, elapsedSeconds = 35).score shouldBe 94
    }

    @Test
    fun `clock never runs backwards`() {
        val state = emptyState().copy(score = 100, elapsedSeconds = 40)

        engine.tick(state, elapsedSeconds = 40) shouldBe state
        engine.tick(state, elapsedSeconds = 12) shouldBe state
    }

    @Test
    fun `penalty is charged exactly when a period completes`() {
        val state = emptyState().copy(score = 100, elapsedSeconds = 9)

        engine.tick(state, elapsedSeconds = 10).score shouldBe 98
    }

    @Test
    fun `a game with an incomplete foundation is not won`() {
        val almost = emptyState()
            .withFoundation(0, suitRun('C'))
            .withFoundation(1, suitRun('D'))
            .withFoundation(2, suitRun('H'))
            .withFoundation(3, suitRun('S', upTo = 12))

        almost.isWon shouldBe false
    }

    @Test
    fun `exposes the rule set's legality and legal moves`() {
        val state = emptyState().copy(stock = down("2C"))

        engine.isLegal(state, Move.Draw) shouldBe true
        engine.isLegal(state, Move.Recycle) shouldBe false
        engine.legalMoves(state) shouldBe listOf(Move.Draw)
    }

    @Test
    fun `applied outcome reports the scoring events`() {
        val state = emptyState().copy(waste = up("AH"))

        val outcome = engine.apply(state, Move.WasteToFoundation(0)).shouldBeInstanceOf<MoveOutcome.Applied>()

        outcome.events shouldBe listOf(io.github.vinaooo.solo.domain.scoring.ScoreEvent.WasteToFoundation)
    }

    @Test
    fun `clock stops once the game is won`() {
        val won = emptyState().copy(elapsedSeconds = 50, score = 10)
            .withFoundation(0, suitRun('C'))
            .withFoundation(1, suitRun('D'))
            .withFoundation(2, suitRun('H'))
            .withFoundation(3, suitRun('S'))

        engine.tick(won, elapsedSeconds = 90) shouldBe won
    }
}
