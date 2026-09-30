package io.github.vinaooo.solo.domain.serialization

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.history.UndoHistory
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GameSerializationTest {

    private val json = Json

    @Test
    fun `game state survives a JSON round trip`() {
        val state = Dealer().deal(SeededShuffler(3), DrawMode.THREE).copy(score = 42, moves = 7, recycles = 2)

        json.decodeFromString<GameState>(json.encodeToString(state)) shouldBe state
    }

    @Test
    fun `undo history survives a JSON round trip and still undoes`() {
        val start = Dealer().deal(SeededShuffler(3), DrawMode.ONE)
        val next = (GameEngine().apply(start, Move.Draw) as MoveOutcome.Applied).state
        val history = UndoHistory().push(start, next)

        val restored = json.decodeFromString<UndoHistory>(json.encodeToString(history))

        restored shouldBe history
        restored.undo(next)?.first?.stock shouldBe start.stock
    }
}
