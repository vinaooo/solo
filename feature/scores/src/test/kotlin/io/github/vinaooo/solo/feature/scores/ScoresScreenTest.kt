package io.github.vinaooo.solo.feature.scores

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScoresScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `lists scores with stats and goes back`() {
        var back = false
        compose.setContent {
            SoloTheme {
                ScoresScreen(
                    uiState = ScoresUiState(
                        scores = listOf(ScoreRecord(4321, 185, 97, DrawMode.THREE, 0)),
                        stats = GameStats(played = 4, won = 3, currentStreak = 2, bestStreak = 3),
                        isLoading = false,
                    ),
                    onBack = { back = true },
                )
            }
        }

        compose.onNodeWithText("4321").assertExists()
        compose.onNodeWithText("3:05").assertExists()
        compose.onNodeWithText("75%").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        back shouldBe true
    }

    @Test
    fun `explains when there are no scores yet`() {
        compose.setContent { SoloTheme { ScoresScreen(ScoresUiState(isLoading = false), onBack = {}) } }

        compose.onNodeWithText("Win a game to see your scores here.").assertExists()
    }
}
