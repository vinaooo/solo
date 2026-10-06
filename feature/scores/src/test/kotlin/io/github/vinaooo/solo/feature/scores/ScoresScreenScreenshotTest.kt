package io.github.vinaooo.solo.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.model.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ScoresScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    // Noon UTC, so the dates read the same in any time zone within ±11 h.
    private val filled = ScoresUiState(
        scores = listOf(
            ScoreRecord(
                points = 5812,
                elapsedSeconds = 148,
                moves = 104,
                drawMode = DrawMode.ONE,
                playedAtMillis = 1790510400000L,
                difficulty = Difficulty.EASY,
            ),
            ScoreRecord(
                points = 4321,
                elapsedSeconds = 185,
                moves = 97,
                drawMode = DrawMode.THREE,
                playedAtMillis = 1789905600000L,
                difficulty = Difficulty.NORMAL,
            ),
            ScoreRecord(
                points = 2750,
                elapsedSeconds = 402,
                moves = 131,
                drawMode = DrawMode.ONE,
                playedAtMillis = 1789214400000L,
            ),
        ),
        stats = GameStats(played = 12, won = 3, currentStreak = 1, bestStreak = 2),
        isLoading = false,
    )

    private fun capture(name: String, uiState: ScoresUiState, themeMode: ThemeMode) {
        compose.setContent {
            SoloTheme(themeMode = themeMode, dynamicColor = false) { ScoresScreen(uiState, onBack = {}) }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun scores_light() = capture("scores_light", filled, ThemeMode.LIGHT)

    @Test
    fun scores_dark() = capture("scores_dark", filled, ThemeMode.DARK)

    @Test
    fun scores_vegas_tabs() = capture(
        "scores_vegas_tabs",
        ScoresUiState(
            scores = listOf(
                ScoreRecord(83, 512, 140, DrawMode.ONE, 1790510400000L, GameMode.VEGAS),
                ScoreRecord(-17, 301, 88, DrawMode.THREE, 1789905600000L, GameMode.VEGAS),
            ),
            stats = GameStats(played = 12, won = 3, currentStreak = 1, bestStreak = 2),
            isLoading = false,
            modes = listOf(GameMode.STANDARD, GameMode.VEGAS, GameMode.COUNTER_TIME),
            mode = GameMode.VEGAS,
        ),
        ThemeMode.LIGHT,
    )

    @Test
    fun scores_counter_time() = capture(
        "scores_counter_time",
        ScoresUiState(
            scores = listOf(ScoreRecord(1200, 233, 97, DrawMode.ONE, 1790510400000L, GameMode.COUNTER_TIME)),
            isLoading = false,
            modes = listOf(GameMode.STANDARD, GameMode.COUNTER_TIME),
            mode = GameMode.COUNTER_TIME,
        ),
        ThemeMode.DARK,
    )

    @Test
    fun scores_empty() = capture("scores_empty", ScoresUiState(isLoading = false), ThemeMode.LIGHT)
}
