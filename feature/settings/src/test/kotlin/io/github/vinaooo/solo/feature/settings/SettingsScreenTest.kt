package io.github.vinaooo.solo.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.ThemeMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<SettingsChange>()
    private var app = AppSettings()

    private fun show() = compose.setContent {
        SoloTheme {
            SettingsScreen(Settings(), app, onChange = { changes += it }, onAppChange = { app = it(app) }, onBack = {})
        }
    }

    @Test
    fun `the game section comes first and reports Solo's changes`() {
        show()

        compose.onNodeWithText("Game").assertExists()
        compose.onNodeWithText("Draw 3").performScrollTo().performClick()
        compose.onNodeWithText("Easy").performScrollTo().performClick()

        changes shouldContainExactly listOf(
            SettingsChange.DrawModeChanged(DrawMode.THREE),
            SettingsChange.DifficultyChanged(Difficulty.EASY),
        )
    }

    @Test
    fun `a scoring mode is chosen by its icon, and its name and meaning show below`() {
        show()

        compose.onNodeWithText("Points, with a time penalty and a speed bonus").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Counter time").performScrollTo().performClick()

        changes shouldContainExactly listOf(SettingsChange.GameModeChanged(GameMode.COUNTER_TIME))
    }

    @Test
    fun `the common settings are vinkit's, and change its settings`() {
        show()

        compose.onNodeWithText("Dark").performScrollTo().performClick()

        app.themeMode shouldBe ThemeMode.DARK
        changes shouldBe emptyList()
    }
}
