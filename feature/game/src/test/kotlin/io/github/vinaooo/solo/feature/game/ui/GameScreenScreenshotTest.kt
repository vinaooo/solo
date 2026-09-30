package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.feature.game.GameUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = PHONE)
class GameScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, themeMode: ThemeMode = ThemeMode.LIGHT, drawMode: DrawMode = DrawMode.ONE) {
        val session = midGame(drawMode)
        compose.setContent {
            SoloTheme(themeMode = themeMode, dynamicColor = false) {
                GameScreen(
                    uiState = GameUiState(session = session, hint = HintEngine().bestHint(session.state)),
                    onIntent = {},
                    onOpenScores = {},
                    onOpenSettings = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun game_phone_light() = capture("game_phone_light")

    @Test
    fun game_phone_dark() = capture("game_phone_dark", ThemeMode.DARK)

    @Test
    fun game_phone_draw_three() = capture("game_phone_draw_three", drawMode = DrawMode.THREE)

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun game_phone_landscape() = capture("game_phone_landscape")

    @Test
    @Config(qualifiers = TABLET)
    fun game_tablet() = capture("game_tablet")

    @Test
    @Config(qualifiers = "pt-rBR-$PHONE")
    fun game_phone_pt_br() = capture("game_phone_pt_br")

    private companion object {
        const val SEED = 3L
        const val HINTED_MOVES = 25

        /** A game some moves in, always the same: a fixed deal played by following the hints. */
        fun midGame(drawMode: DrawMode): GameSession {
            val engine = GameEngine()
            val hints = HintEngine()
            var session = GameSession(SEED, Dealer().deal(SeededShuffler(SEED), drawMode))
            repeat(HINTED_MOVES) {
                session = hints.bestHint(session.state)?.let { session.play(it, engine) } ?: session
            }
            return session
        }
    }
}

private const val PHONE = "w411dp-h891dp-xhdpi"
private const val PHONE_LANDSCAPE = "w891dp-h411dp-land-xhdpi"
private const val TABLET = "w1280dp-h800dp-land-mdpi"
