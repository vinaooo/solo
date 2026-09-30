package io.github.vinaooo.solo.core.designsystem

import androidx.test.core.app.ApplicationProvider
import io.github.vinaooo.solo.core.designsystem.theme.BrandColors
import io.github.vinaooo.solo.core.designsystem.theme.colorSchemeFor
import io.github.vinaooo.solo.core.designsystem.theme.isDarkTheme
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeSelectionTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `system mode follows the device, explicit modes override it`() {
        isDarkTheme(ThemeMode.SYSTEM, systemInDark = true) shouldBe true
        isDarkTheme(ThemeMode.SYSTEM, systemInDark = false) shouldBe false
        isDarkTheme(ThemeMode.DARK, systemInDark = false) shouldBe true
        isDarkTheme(ThemeMode.LIGHT, systemInDark = true) shouldBe false
    }

    @Test
    fun `brand colors are used when dynamic color is off`() {
        colorSchemeFor(context, darkTheme = false, dynamicColor = false) shouldBe BrandColors.light
        colorSchemeFor(context, darkTheme = true, dynamicColor = false) shouldBe BrandColors.dark
    }

    @Test
    fun `dynamic color is used on Android 12 and later`() {
        colorSchemeFor(context, darkTheme = false, dynamicColor = true) shouldNotBe BrandColors.light
        colorSchemeFor(context, darkTheme = true, dynamicColor = true) shouldNotBe BrandColors.dark
    }

    @Test
    @Config(sdk = [30])
    fun `older devices fall back to brand colors even with dynamic color on`() {
        colorSchemeFor(context, darkTheme = false, dynamicColor = true) shouldBe BrandColors.light
        colorSchemeFor(context, darkTheme = true, dynamicColor = true) shouldBe BrandColors.dark
    }
}
