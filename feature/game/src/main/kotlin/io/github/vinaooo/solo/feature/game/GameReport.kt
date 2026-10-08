package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.BoardCodec
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.vinkit.bugreport.GameReport
import io.github.vinaooo.vinkit.bugreport.ReportTarget
import kotlinx.serialization.json.Json

/** Where Solo's bug reports go: the contact in the privacy policy, chosen with the user, and the repository. */
internal val SoloReports = ReportTarget("vrpedrinho+solo@gmail.com", "vinaooo/solo")

/** What a bug report says about Solo: its settings and, with a game, the game, its exact board and its file. */
internal fun gameReport(settings: Settings, session: GameSession?): GameReport = GameReport(
    details = listOfNotNull(
        with(settings) {
            "Settings: $drawMode, $gameMode, $difficulty, $handedness hand, board $boardAlignment, theme $themeMode, " +
                "dynamic color $dynamicColor, phone view $phoneView"
        },
        session?.run {
            "Game: seed $seed, ${state.mode}, ${state.drawMode}, ${state.difficulty}, ${state.moves} moves, " +
                "score ${state.score}, ${state.elapsedSeconds}s"
        },
    ),
    state = session?.let { BoardCodec.encode(it.state) },
    files = session?.let { mapOf("game.json" to Json.encodeToString(it)) }.orEmpty(),
)
