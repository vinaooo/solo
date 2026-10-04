package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.hint.DeadEndDetector
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.scoring.ScoreEvent
import io.github.vinaooo.solo.domain.scoring.StandardScoring
import io.github.vinaooo.solo.domain.scoring.VegasScoring
import io.github.vinaooo.solo.domain.scoring.scoringFor
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GameModeRulesTest {

    private val engine = GameEngine()

    @Test
    fun `each mode's limits and starting score`() {
        GameMode.STANDARD.recycleLimit(DrawMode.ONE).shouldBeNull()
        GameMode.COUNTER_TIME.recycleLimit(DrawMode.THREE).shouldBeNull()
        GameMode.VEGAS.recycleLimit(DrawMode.ONE) shouldBe 0
        GameMode.VEGAS_CUMULATIVE.recycleLimit(DrawMode.THREE) shouldBe 2

        GameMode.COUNTER_TIME.timeLimitSeconds(DrawMode.ONE) shouldBe 600
        GameMode.COUNTER_TIME.timeLimitSeconds(DrawMode.THREE) shouldBe 900
        GameMode.VEGAS.timeLimitSeconds(DrawMode.ONE).shouldBeNull()
        GameMode.STANDARD.timeLimitSeconds(DrawMode.THREE).shouldBeNull()

        GameMode.STANDARD.startingScore(bank = 99) shouldBe 0
        GameMode.COUNTER_TIME.startingScore(bank = 99) shouldBe 0
        GameMode.VEGAS.startingScore(bank = 99) shouldBe -52
        GameMode.VEGAS_CUMULATIVE.startingScore(bank = 99) shouldBe 47

        GameMode.entries.filter { it.isVegas } shouldBe listOf(GameMode.VEGAS, GameMode.VEGAS_CUMULATIVE)
    }

    @Test
    fun `vegas pays $5 a card on a foundation, takes it back when it leaves, and ignores the rest`() {
        val vegas = VegasScoring()
        vegas.pointsFor(ScoreEvent.WasteToFoundation) shouldBe 5
        vegas.pointsFor(ScoreEvent.TableauToFoundation) shouldBe 5
        vegas.pointsFor(ScoreEvent.FoundationToTableau) shouldBe -5
        listOf(
            ScoreEvent.WasteToTableau,
            ScoreEvent.CardRevealed,
            ScoreEvent.Undo,
            ScoreEvent.Recycle(DrawMode.ONE, 1),
            ScoreEvent.TimeElapsed(3),
            ScoreEvent.Won(100),
        ).forEach { vegas.pointsFor(it) shouldBe 0 }
        vegas.bounded(-52) shouldBe -52
        StandardScoring().bounded(-52) shouldBe 0
        scoringFor(GameMode.COUNTER_TIME).shouldBeInstanceOf<StandardScoring>()
        scoringFor(GameMode.VEGAS_CUMULATIVE).shouldBeInstanceOf<VegasScoring>()
    }

    @Test
    fun `a vegas score stays below zero`() {
        val state = emptyState().copy(mode = GameMode.VEGAS, score = -52, waste = up("AH"))

        val played = engine.apply(state, Move.WasteToFoundation(0)).shouldBeInstanceOf<MoveOutcome.Applied>().state

        played.score shouldBe -47
    }

    @Test
    fun `vegas draw 1 goes through the stock once`() {
        val state = emptyState().copy(mode = GameMode.VEGAS, waste = up("2H", "3H"))

        state.canRecycle shouldBe false
        engine.apply(state, Move.Recycle) shouldBe MoveOutcome.Rejected
        engine.apply(state.copy(mode = GameMode.STANDARD), Move.Recycle).shouldBeInstanceOf<MoveOutcome.Applied>()
    }

    @Test
    fun `vegas draw 3 goes through the stock three times`() {
        var state = emptyState(DrawMode.THREE).copy(mode = GameMode.VEGAS_CUMULATIVE, waste = up("2H", "3H"))
        repeat(2) {
            state = engine.apply(state, Move.Recycle).shouldBeInstanceOf<MoveOutcome.Applied>().state
            state = engine.apply(state, Move.Draw).shouldBeInstanceOf<MoveOutcome.Applied>().state
        }

        state.recycles shouldBe 2
        engine.apply(state, Move.Recycle) shouldBe MoveOutcome.Rejected
    }

    @Test
    fun `against the clock, time stops at the limit and the game takes no more moves`() {
        val state = emptyState().copy(mode = GameMode.COUNTER_TIME, moves = 3, stock = down("5C"))
        state.secondsLeft shouldBe 600

        val almost = engine.tick(state, 599)
        almost.secondsLeft shouldBe 1
        almost.isTimeUp shouldBe false

        val over = engine.tick(almost, 650)
        over.elapsedSeconds shouldBe 600
        over.secondsLeft shouldBe 0
        over.isTimeUp shouldBe true
        engine.apply(over, Move.Draw) shouldBe MoveOutcome.Rejected
        engine.tick(over, 700) shouldBe over
        emptyState().secondsLeft.shouldBeNull()
    }

    @Test
    fun `a won game is never out of time`() {
        val won = (0 until 4).fold(emptyState().copy(mode = GameMode.COUNTER_TIME, elapsedSeconds = 600)) { s, f ->
            s.withFoundation(f, io.github.vinaooo.solo.domain.suitRun("CDHS"[f]))
        }

        won.isTimeUp shouldBe false
    }

    @Test
    fun `vegas undo costs nothing and doesn't lift the score to zero`() {
        val start = GameSession(1, emptyState().copy(mode = GameMode.VEGAS, score = -52, waste = up("AH")))

        val played = start.play(Move.WasteToFoundation(0), engine)!!
        played.state.score shouldBe -47
        played.undo()!!.state.score shouldBe -52
        played.undo()!!.redo()!!.state.score shouldBe -47
    }

    @Test
    fun `with limited passes, a waste that can't go back is a dead end`() = runTest {
        // The ace is under the 5 in the waste: turned over and dealt again (Standard) it comes up and goes to a
        // foundation; with no pass left (Vegas Draw 1) it never does.
        val state: GameState = emptyState()
            .copy(waste = up("AH", "5S"), moves = 4)
            .withTableau(0, up("KD"))

        DeadEndDetector().isStuck(state) shouldBe false
        DeadEndDetector().isStuck(state.copy(mode = GameMode.VEGAS)) shouldBe true
    }
}
