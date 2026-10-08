package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.BoardCodec
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.session.decodeSession
import io.github.vinaooo.vinkit.core.AppSettings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BugReportTest {

    private val session = GameSession(
        seed = 77,
        state = Dealer().deal(SeededShuffler(77), DrawMode.THREE)
            .copy(mode = GameMode.VEGAS, difficulty = Difficulty.EASY, moves = 12, score = -32),
    )
    private val settings = Settings(drawMode = DrawMode.THREE, gameMode = GameMode.VEGAS)

    @Test
    fun `a report holds the settings, the game, its exact board and its file`() {
        val report = gameReport(settings, AppSettings(), session)

        report.details shouldContainExactly listOf(
            "Settings: THREE, VEGAS, NORMAL, RIGHT hand, board TOP, theme SYSTEM, dynamic color true, phone view false",
            "Game: seed 77, VEGAS, THREE, EASY, 12 moves, score -32, 0s",
        )
        BoardCodec.decode(report.state!!) shouldBe session.state
        decodeSession(report.files.getValue("game.json")) shouldBe session
    }

    @Test
    fun `no game means no game line, board or file`() {
        val report = gameReport(settings, AppSettings(), null)

        report.details.size shouldBe 1
        report.state shouldBe null
        report.files shouldBe emptyMap()
    }
}
