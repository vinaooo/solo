package io.github.vinaooo.solo.feature.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `controls report the change the player made`() {
        val changes = mutableListOf<SettingsChange>()
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = { changes += it }, onBack = {}) } }

        compose.onNodeWithText("Draw 3").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performScrollTo().performClick()
        compose.onNodeWithText("Sound effects").performScrollTo().performClick()
        compose.onNodeWithText("Vibration").performScrollTo().performClick()
        compose.onNodeWithText("Show timer").performScrollTo().performClick()

        changes shouldContainExactly listOf(
            SettingsChange.DrawModeChanged(DrawMode.THREE),
            SettingsChange.ThemeModeChanged(ThemeMode.DARK),
            SettingsChange.SoundChanged(false),
            SettingsChange.HapticsChanged(false),
            SettingsChange.ShowTimerChanged(false),
        )
    }

    @Test
    fun `privacy options open the consent form where the law requires them`() {
        var opened = 0
        compose.setContent {
            SoloTheme {
                SettingsScreen(Settings(), onChange = {}, onBack = {}, privacyOptionsRequired = true) { opened++ }
            }
        }

        compose.onNodeWithText("Privacy options").performScrollTo().performClick()

        opened shouldBe 1
    }

    @Test
    fun `privacy options are hidden where no consent is required`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        compose.onNodeWithText("Privacy options").assertDoesNotExist()
        compose.onNodeWithText("Privacy").assertDoesNotExist()
    }

    @Test
    fun `dynamic color can be turned off on Android 12 and later`() {
        val changes = mutableListOf<SettingsChange>()
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = { changes += it }, onBack = {}) } }

        compose.onNodeWithText("Dynamic color").performScrollTo().performClick()

        changes shouldContainExactly listOf(SettingsChange.DynamicColorChanged(false))
    }

    @Test
    fun `section titles are headings, so TalkBack can jump between them`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        listOf("Game", "Appearance", "Feedback").forEach {
            compose.onNode(hasText(it) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        }
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `Brazilian Portuguese names the feedback section after what it holds`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        compose.onNodeWithText("Sons e vibração").performScrollTo().assertExists()
        compose.onNodeWithText("Vale a partir da próxima partida.").performScrollTo().assertExists()
    }
}
