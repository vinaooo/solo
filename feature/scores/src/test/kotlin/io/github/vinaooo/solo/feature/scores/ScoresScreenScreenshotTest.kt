package io.github.vinaooo.solo.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.scores.ModeSection
import io.github.vinaooo.vinkit.scores.ScoresUiState
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

    private val stats = GameStats(played = 12, won = 3, currentStreak = 1, bestStreak = 2)

    // Noon UTC, so the dates read the same in any time zone within ±11 h.
    private val filled = state(
        GameMode.STANDARD,
        ModeSection(
            "STANDARD",
            stats,
            listOf(
                soloRecord(5812, 148, 104, DrawMode.ONE, 1790510400000L, difficulty = Difficulty.EASY),
                soloRecord(4321, 185, 97, DrawMode.THREE, 1789905600000L, difficulty = Difficulty.NORMAL),
                soloRecord(2750, 402, 131, DrawMode.ONE, 1789214400000L),
            ),
        ),
    )

    private fun state(group: GameMode, section: ModeSection, vararg groups: GameMode = arrayOf(group)) = ScoresUiState(
        isLoading = false,
        groups = groups.map {
            it.name
        },
        group = group.name,
        sections = listOf(section),
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
        state(
            GameMode.VEGAS,
            ModeSection(
                "VEGAS",
                stats,
                listOf(
                    soloRecord(83, 512, 140, DrawMode.ONE, 1790510400000L, GameMode.VEGAS),
                    soloRecord(-17, 301, 88, DrawMode.THREE, 1789905600000L, GameMode.VEGAS),
                ),
            ),
            GameMode.STANDARD,
            GameMode.VEGAS,
            GameMode.COUNTER_TIME,
        ),
        ThemeMode.LIGHT,
    )

    @Test
    fun scores_counter_time() = capture(
        "scores_counter_time",
        state(
            GameMode.COUNTER_TIME,
            ModeSection(
                "COUNTER_TIME",
                GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1),
                listOf(soloRecord(1200, 233, 97, DrawMode.ONE, 1790510400000L, GameMode.COUNTER_TIME)),
                Ranking.FASTEST,
            ),
            GameMode.STANDARD,
            GameMode.COUNTER_TIME,
        ),
        ThemeMode.DARK,
    )

    @Test
    fun scores_empty() = capture("scores_empty", ScoresUiState(isLoading = false), ThemeMode.LIGHT)
}
