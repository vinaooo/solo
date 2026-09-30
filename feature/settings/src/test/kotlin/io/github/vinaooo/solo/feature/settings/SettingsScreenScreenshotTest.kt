package io.github.vinaooo.solo.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Settings
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
class SettingsScreenScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, themeMode: ThemeMode) {
        compose.setContent {
            SoloTheme(themeMode = themeMode, dynamicColor = false) {
                SettingsScreen(Settings(themeMode = themeMode), onChange = {}, onBack = {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun settings_light() = capture("settings_light", ThemeMode.LIGHT)

    @Test
    fun settings_dark() = capture("settings_dark", ThemeMode.DARK)

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-xhdpi")
    fun settings_pt_br() = capture("settings_pt_br", ThemeMode.LIGHT)
}
