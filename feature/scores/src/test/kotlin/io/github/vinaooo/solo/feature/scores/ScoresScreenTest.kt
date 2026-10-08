package io.github.vinaooo.solo.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.scores.ModeSection
import io.github.vinaooo.vinkit.scores.ScoresUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ScoresScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(mode: GameMode, vararg records: ScoreRecord, bank: Int = 0) = compose.setContent {
        SoloTheme {
            ScoresScreen(
                ScoresUiState(
                    isLoading = false,
                    groups = listOf(mode.name),
                    group = mode.name,
                    sections = listOf(ModeSection(mode.name, GameStats(played = 4, won = 3), records.toList())),
                ),
                onBack = {},
                bank = bank,
            )
        }
    }

    @Test
    fun `a score shows its moves, draw mode and difficulty`() {
        show(GameMode.STANDARD, soloRecord(4321, 185, 97, DrawMode.THREE, 0, difficulty = Difficulty.EASY))

        compose.onNodeWithText("Standard").assertExists()
        compose.onNodeWithText("4321").assertExists()
        compose.onNodeWithText("97 moves · Draw 3 · Easy", substring = true).assertExists()
    }

    @Test
    fun `Vegas scores are dollars, read as dollars`() {
        show(GameMode.VEGAS, soloRecord(-17, 301, 88, DrawMode.ONE, 0, GameMode.VEGAS))

        compose.onNodeWithText("-$17").assertExists()
        compose.onNodeWithContentDescription("-17 dollars").assertExists()
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `Brazilian Portuguese counts jogadas`() {
        show(GameMode.STANDARD, soloRecord(4321, 185, 97, DrawMode.ONE, 0))

        compose.onNodeWithText("97 jogadas · Virar 1 · Difícil", substring = true).assertExists()
    }

    @Test
    fun `cumulative Vegas shows its balance under its stats, and its games in dollars`() {
        show(
            GameMode.VEGAS_CUMULATIVE,
            soloRecord(-32, 400, 90, DrawMode.ONE, 0, GameMode.VEGAS_CUMULATIVE),
            bank = -104,
        )

        compose.onNodeWithText("Balance: -$104").assertExists()
        compose.onNodeWithText("-$32").assertExists()
    }
}
