package io.github.vinaooo.solo.domain.session

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GameCodecTest {

    private val game = (1..40).fold(
        GameSession(9, Dealer().deal(SeededShuffler(9), DrawMode.THREE).copy(mode = GameMode.VEGAS, score = -52)),
    ) { session, _ -> HintEngine().bestHint(session.state)?.let { session.play(it, GameEngine()) } ?: session }

    @Test
    fun `a board survives the trip through its code, short enough for an issue link`() {
        val code = GameCodec.encode(game.state)

        GameCodec.decode(code) shouldBe game.state
        code.length shouldBeLessThan 2_000
    }

    @Test
    fun `a code wrapped over lines, as an issue shows it, still reads`() {
        val wrapped = GameCodec.encode(game.state).chunked(60).joinToString("\n", prefix = "  ", postfix = "\n")

        GameCodec.decode(wrapped) shouldBe game.state
    }

    @Test
    fun `a report's game file reads back as the whole game`() {
        GameCodec.decodeSession(Json.encodeToString(game)) shouldBe game
    }
}
