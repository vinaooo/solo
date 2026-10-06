package io.github.vinaooo.solo.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.PhoneViewSide
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeColor
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
        compose.onNodeWithText("Left").performScrollTo().performClick()

        changes shouldContainExactly listOf(
            SettingsChange.DrawModeChanged(DrawMode.THREE),
            SettingsChange.ThemeModeChanged(ThemeMode.DARK),
            SettingsChange.SoundChanged(false),
            SettingsChange.HapticsChanged(false),
            SettingsChange.HandednessChanged(Handedness.LEFT),
        )
    }

    @Test
    fun `the colors show only once dynamic color is off`() {
        var settings by mutableStateOf(Settings())
        val changes = mutableListOf<SettingsChange>()
        compose.setContent { SoloTheme { SettingsScreen(settings, onChange = { changes += it }, onBack = {}) } }

        compose.onNodeWithContentDescription("Purple").assertDoesNotExist()
        settings = Settings(dynamicColor = false)
        compose.onNodeWithContentDescription("Purple").performScrollTo().performClick()

        changes shouldContainExactly listOf(SettingsChange.ThemeColorChanged(ThemeColor.PURPLE))
    }

    @Test
    @Config(sdk = [30])
    fun `without dynamic color, the colors are always there`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        compose.onNodeWithContentDescription("Purple").performScrollTo().assertExists()
    }

    @Test
    fun `a scoring mode is chosen by its icon, and its name and meaning show below`() {
        val changes = mutableListOf<SettingsChange>()
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = { changes += it }, onBack = {}) } }

        compose.onNodeWithText("Points, with a time penalty and a speed bonus").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Counter time").performScrollTo().performClick()

        changes shouldContainExactly listOf(SettingsChange.GameModeChanged(GameMode.COUNTER_TIME))
    }

    @Test
    fun `a phone has no phone view`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        compose.onNodeWithText("Phone view").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `on a tablet, the board side shows only once phone view is on`() {
        var settings by mutableStateOf(Settings())
        val changes = mutableListOf<SettingsChange>()
        compose.setContent { SoloTheme { SettingsScreen(settings, onChange = { changes += it }, onBack = {}) } }

        compose.onNodeWithText("Board side").assertDoesNotExist()
        compose.onNodeWithText("Phone view").performScrollTo().performClick()
        settings = Settings(phoneView = true)
        // The first "Right": the preferred hand's comes after it, in Appearance.
        compose.onAllNodesWithText("Right")[0].performScrollTo().performClick()

        changes shouldContainExactly listOf(
            SettingsChange.PhoneViewChanged(true),
            SettingsChange.PhoneViewSideChanged(PhoneViewSide.RIGHT),
        )
    }

    @Test
    fun `privacy options open the consent form where the law requires them`() {
        var opened = 0
        compose.setContent {
            SoloTheme {
                SettingsScreen(
                    Settings(),
                    onChange = {},
                    onBack = {},
                    privacyOptionsRequired = true,
                    onOpenPrivacyOptions = { opened++ },
                )
            }
        }

        compose.onNodeWithText("Privacy options").performScrollTo().performClick()

        opened shouldBe 1
    }

    @Test
    fun `privacy options are hidden where no consent is required`() {
        compose.setContent { SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}) } }

        compose.onNodeWithText("Privacy options").assertDoesNotExist()
    }

    @Test
    fun `the privacy policy link is always there, as Google Play requires`() {
        var opened = 0
        compose.setContent {
            SoloTheme { SettingsScreen(Settings(), onChange = {}, onBack = {}, onOpenPrivacyPolicy = { opened++ }) }
        }

        compose.onNodeWithText("Privacy policy").performScrollTo().performClick()

        opened shouldBe 1
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
    }
}

private const val TABLET = "w800dp-h1280dp-port-mdpi"
