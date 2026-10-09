package io.github.vinaooo.solo.domain.session

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GameSessionTest {

    private val engine = GameEngine()
    private val start = GameSession(seed = 9, state = emptyState().copy(stock = down("2C", "3C")))

    @Test
    fun `playing a legal move advances the state and records history`() {
        val next = start.play(Move.Draw, engine).shouldNotBeNull()

        next.state.waste.size shouldBe 1
        next.canUndo.shouldBeTrue()
        next.seed shouldBe 9
    }

    @Test
    fun `an illegal move is rejected`() {
        start.play(Move.Recycle, engine).shouldBeNull()
    }

    @Test
    fun `undo goes back one move`() {
        val undone = start.play(Move.Draw, engine)!!.undo().shouldNotBeNull()

        undone.state.stock shouldBe start.state.stock
        undone.canUndo.shouldBeFalse()
    }

    @Test
    fun `undos are counted, and a redo doesn't take them back`() {
        val twice = start.play(Move.Draw, engine)!!.undo()!!.redo()!!.undo().shouldNotBeNull()

        twice.undos shouldBe 2
    }

    @Test
    fun `undo with no history does nothing`() {
        start.undo().shouldBeNull()
    }

    @Test
    fun `tick advances the clock`() {
        start.tick(15, engine).state.elapsedSeconds shouldBe 15
    }

    @Test
    fun `a session with moves is in progress, a fresh or won one is not`() {
        start.isInProgress.shouldBeFalse()
        start.play(Move.Draw, engine)!!.isInProgress.shouldBeTrue()
    }

    @Test
    fun `session with history survives a JSON round trip`() {
        val played = start.play(Move.Draw, engine)!!

        Json.decodeFromString<GameSession>(Json.encodeToString(played)) shouldBe played
    }
}
