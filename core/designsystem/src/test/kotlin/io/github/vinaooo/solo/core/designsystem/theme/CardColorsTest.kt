package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.ui.graphics.luminance
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.paletteScheme
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardColorsTest {

    private val light = cardColorsFor(paletteScheme(ThemeColor.GREEN, dark = false), darkTheme = false)
    private val dark = cardColorsFor(paletteScheme(ThemeColor.GREEN, dark = true), darkTheme = true)

    @Test
    fun `spades and clubs take the toolbar icon color, pale on dark faces and dark on light ones`() {
        dark.blackSuits shouldBe paletteScheme(ThemeColor.GREEN, dark = true).onPrimaryContainer
        light.blackSuits shouldBe paletteScheme(ThemeColor.GREEN, dark = false).onPrimaryContainer
        dark.blackSuits.luminance() shouldBeGreaterThan dark.face.luminance()
        light.blackSuits.luminance() shouldBeLessThan light.face.luminance()
    }

    @Test
    fun `hearts and diamonds are a shade of the accent`() {
        (dark.redSuits.hue() - paletteScheme(ThemeColor.GREEN, dark = true).primary.hue()).shouldBeAbout(0f)
        (light.redSuits.hue() - paletteScheme(ThemeColor.GREEN, dark = false).primary.hue()).shouldBeAbout(0f)
    }

    @Test
    fun `both suit inks stay readable on their card face and apart from each other`() {
        listOf(light, dark).forEach { colors ->
            contrast(colors.redSuits, colors.face) shouldBeGreaterThanOrEqual MIN_GRAPHIC_CONTRAST
            contrast(colors.blackSuits, colors.face) shouldBeGreaterThanOrEqual MIN_GRAPHIC_CONTRAST
            contrast(colors.redSuits, colors.blackSuits) shouldBeGreaterThanOrEqual MIN_GRAPHIC_CONTRAST
        }
    }

    private fun Float.shouldBeAbout(expected: Float) = (kotlin.math.abs(this - expected) < HUE_TOLERANCE) shouldBe true

    private companion object {
        const val MIN_GRAPHIC_CONTRAST = 3f
        const val HUE_TOLERANCE = 2f
    }
}
