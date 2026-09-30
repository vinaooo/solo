package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.ui.graphics.luminance
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardColorsTest {

    private val light = cardColorsFor(BrandColors.light, darkTheme = false)
    private val dark = cardColorsFor(BrandColors.dark, darkTheme = true)

    @Test
    fun `spades and clubs take the toolbar icon color, pale on dark faces and dark on light ones`() {
        dark.blackSuits shouldBe BrandColors.dark.onPrimaryContainer
        light.blackSuits shouldBe BrandColors.light.onPrimaryContainer
        dark.blackSuits.luminance() shouldBeGreaterThan dark.face.luminance()
        light.blackSuits.luminance() shouldBeLessThan light.face.luminance()
    }

    @Test
    fun `hearts and diamonds are a shade of the accent`() {
        (dark.redSuits.hue() - BrandColors.dark.primary.hue()).shouldBeAbout(0f)
        (light.redSuits.hue() - BrandColors.light.primary.hue()).shouldBeAbout(0f)
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
