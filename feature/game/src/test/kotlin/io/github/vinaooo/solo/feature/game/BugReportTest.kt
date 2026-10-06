package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.GameCodec
import io.github.vinaooo.solo.domain.session.GameSession
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.net.URLDecoder
import org.junit.jupiter.api.Test

class BugReportTest {

    private val session = GameSession(
        seed = 77,
        state = Dealer().deal(SeededShuffler(77), DrawMode.THREE)
            .copy(mode = GameMode.VEGAS, difficulty = Difficulty.EASY, moves = 12, score = -32),
    )
    private val info = ReportInfo(
        appVersion = "1.2.0 (140)",
        android = "16 (API 36)",
        device = "Google Pixel 9a",
        screen = "411x891dp, 420dpi",
        settings = Settings(drawMode = DrawMode.THREE, gameMode = GameMode.VEGAS),
        session = session,
    )

    @Test
    fun `the report starts with the player's words, then the facts to reproduce it`() {
        val body = reportBody(info, "The ace didn't move")

        body shouldStartWith "The ace didn't move\n\n---\n"
        body shouldContain "App: 1.2.0 (140)"
        body shouldContain "Android: 16 (API 36)"
        body shouldContain "Device: Google Pixel 9a"
        body shouldContain "Settings: THREE, VEGAS, NORMAL, RIGHT hand"
        body shouldContain "Game: seed 77, VEGAS, THREE, EASY, 12 moves, score -32"
        val code = body.substringAfter("State:\n```\n").substringBefore("\n```")
        GameCodec.decode(code) shouldBe session.state
    }

    @Test
    fun `an empty description says so, and no game means no game line`() {
        val body = reportBody(info.copy(session = null), "  ")

        body shouldStartWith "(no description)"
        body.contains("Game:") shouldBe false
    }

    @Test
    fun `the GitHub link opens a new issue with the title and body filled in`() {
        val url = githubIssueUrl("Ace & king", "line 1\nline 2")

        url shouldStartWith "https://github.com/vinaooo/solo/issues/new?title="
        URLDecoder.decode(url.substringAfter("title=").substringBefore("&body="), "UTF-8") shouldBe "Ace & king"
        URLDecoder.decode(url.substringAfter("&body="), "UTF-8") shouldBe "line 1\nline 2"
    }
}
