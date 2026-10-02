package io.github.vinaooo.solo.domain.history

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UndoHistoryTest {

    private val engine = GameEngine()

    @Test
    fun `empty history cannot undo`() {
        val history = UndoHistory()
        history.canUndo.shouldBeFalse()
        history.undo(emptyState()).shouldBeNull()
    }

    @Test
    fun `undo restores the previous board, keeps the clock and charges the penalty`() {
        val before = emptyState().copy(stock = down("2C", "3C"), score = 100, moves = 2, elapsedSeconds = 10)
        val after = (engine.apply(before, Move.Draw) as MoveOutcome.Applied).state.copy(elapsedSeconds = 40)
        val history = UndoHistory().push(before, after)

        val (restored, remaining) = history.undo(after).shouldNotBeNull()

        restored.stock shouldBe before.stock
        restored.waste shouldBe before.waste
        restored.elapsedSeconds shouldBe 40
        restored.moves shouldBe after.moves
        restored.score shouldBe 100 - 15
        remaining.canUndo.shouldBeFalse()
    }

    @Test
    fun `undo reverses the points the move earned`() {
        val before = emptyState().copy(waste = up("6H"), score = 100).withTableau(0, up("7S"))
        val after = (engine.apply(before, Move.WasteToTableau(0)) as MoveOutcome.Applied).state
        after.score shouldBe 105

        val (restored, _) = UndoHistory().push(before, after).undo(after).shouldNotBeNull()

        restored.score shouldBe 100 - 15
    }

    @Test
    fun `undo keeps time penalties charged since the move`() {
        val before = emptyState().copy(waste = up("6H"), score = 100).withTableau(0, up("7S"))
        val after = (engine.apply(before, Move.WasteToTableau(0)) as MoveOutcome.Applied).state
        val history = UndoHistory().push(before, after)
        val ticked = engine.tick(after, elapsedSeconds = 20) // 105 - 4

        val (restored, _) = history.undo(ticked).shouldNotBeNull()

        restored.score shouldBe 101 - 5 - 15
    }

    @Test
    fun `undo penalty never takes the score below zero`() {
        val before = emptyState().copy(stock = down("2C"))
        val after = (engine.apply(before, Move.Draw) as MoveOutcome.Applied).state

        UndoHistory().push(before, after).undo(after).shouldNotBeNull().first.score shouldBe 0
    }

    @Test
    fun `history is unlimited and last in, first out`() {
        var state = emptyState().copy(stock = down(*Array(20) { "${(it % 9) + 2}C" }))
        var history = UndoHistory()
        val boards = mutableListOf(state.stock)
        repeat(20) {
            val next = (engine.apply(state, Move.Draw) as MoveOutcome.Applied).state
            history = history.push(state, next)
            state = next
            boards += state.stock
        }
        history.size shouldBe 20

        repeat(20) { step ->
            val (restored, remaining) = history.undo(state).shouldNotBeNull()
            restored.stock shouldBe boards[boards.lastIndex - step - 1]
            state = restored
            history = remaining
        }
        history.canUndo.shouldBeFalse()
    }

    @Test
    fun `canUndo is true after a push`() {
        UndoHistory().push(emptyState(), emptyState()).canUndo.shouldBeTrue()
    }

    @Test
    fun `redo plays the undone move again, earning its points back but keeping the penalty`() {
        val before = emptyState().copy(waste = up("6H"), score = 100, moves = 3).withTableau(0, up("7S"))
        val after = (engine.apply(before, Move.WasteToTableau(0)) as MoveOutcome.Applied).state
        val (undone, history) = UndoHistory().push(before, after).undo(after).shouldNotBeNull()
        history.canRedo.shouldBeTrue()

        val (redone, remaining) = history.redo(undone.copy(elapsedSeconds = 30)).shouldNotBeNull()

        redone.tableau shouldBe after.tableau
        redone.waste shouldBe after.waste
        redone.score shouldBe 100 - 15 + 5
        redone.moves shouldBe after.moves + 1
        redone.elapsedSeconds shouldBe 30
        remaining.canRedo.shouldBeFalse()
        remaining.undo(redone).shouldNotBeNull().first.tableau shouldBe before.tableau
    }

    @Test
    fun `a new move clears the redo stack`() {
        val before = emptyState().copy(stock = down("2C", "3C"))
        val after = (engine.apply(before, Move.Draw) as MoveOutcome.Applied).state
        val (undone, history) = UndoHistory().push(before, after).undo(after).shouldNotBeNull()

        val again = (engine.apply(undone, Move.Draw) as MoveOutcome.Applied).state
        history.push(undone, again).canRedo.shouldBeFalse()
    }

    @Test
    fun `empty history cannot redo`() {
        UndoHistory().redo(emptyState()).shouldBeNull()
    }
}
