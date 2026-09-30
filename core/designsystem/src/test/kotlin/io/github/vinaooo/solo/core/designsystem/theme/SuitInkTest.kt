package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SuitInkTest {

    /** The device's own purple palette in the dark theme: the accent and the toolbar icon color are both pale. */
    private val paleAccent = Color(0xFFD7BDE8)
    private val paleIconInk = Color(0xFFF2DBFF)
    private val darkFace = Color(0xFF2B302D)

    private fun colorsWithAccent(accent: Color, darkTheme: Boolean): CardColors {
        val scheme = if (darkTheme) BrandColors.dark else BrandColors.light
        return cardColorsFor(scheme.copy(primary = accent), darkTheme)
    }

    private fun CardColors.shouldKeepSuitsApart() {
        contrast(redSuits, blackSuits) shouldBeGreaterThanOrEqual MIN_CONTRAST
        contrast(redSuits, face) shouldBeGreaterThanOrEqual MIN_CONTRAST
    }

    @Test
    fun `a pale accent next to a pale ink is deepened into a vivid shade of the same hue`() {
        contrast(paleAccent, paleIconInk) shouldBeLessThan MIN_CONTRAST

        val ink = accentSuitInk(paleAccent, neutral = paleIconInk, face = darkFace)

        ink.hue() shouldBe (paleAccent.hue() plusOrMinus HUE_TOLERANCE)
        ink.saturation() shouldBeGreaterThanOrEqual VIVID
        contrast(ink, paleIconInk) shouldBeGreaterThanOrEqual MIN_CONTRAST
        contrast(ink, darkFace) shouldBeGreaterThanOrEqual MIN_CONTRAST
    }

    @Test
    fun `the ink contrasts equally with the other suit and the face`() {
        val ink = accentSuitInk(paleAccent, neutral = paleIconInk, face = darkFace)

        contrast(ink, paleIconInk) shouldBe (contrast(ink, darkFace) plusOrMinus 0.01f)
    }

    @Test
    fun `a grey accent stays grey instead of getting a hue`() {
        val ink = accentSuitInk(PALE_GREY, neutral = paleIconInk, face = darkFace)

        ink.saturation() shouldBeLessThan GREYISH
        contrast(ink, paleIconInk) shouldBeGreaterThanOrEqual MIN_CONTRAST
    }

    @Test
    fun `any accent gives two suit colors that stand apart in both themes`() {
        for (hue in 0 until 360 step HUE_STEP) {
            for (saturation in listOf(0f, 0.1f, 0.3f, 0.6f, 1f)) {
                for (value in listOf(0.3f, 0.6f, 0.9f)) {
                    val accent = Color.hsv(hue.toFloat(), saturation, value)
                    colorsWithAccent(accent, darkTheme = true).shouldKeepSuitsApart()
                    colorsWithAccent(accent, darkTheme = false).shouldKeepSuitsApart()
                }
            }
        }
    }

    @Test
    fun `hue follows the color wheel and is zero for greys`() {
        Color.Red.hue() shouldBe 0f
        Color.Green.hue() shouldBe (120f plusOrMinus 0.01f)
        Color.Blue.hue() shouldBe (240f plusOrMinus 0.01f)
        Color.Magenta.hue() shouldBe (300f plusOrMinus 0.01f)
        PALE_GREY.hue() shouldBe 0f
    }

    @Test
    fun `saturation is zero for greys and one for pure colors`() {
        Color.Black.saturation() shouldBe 0f
        Color.Red.saturation() shouldBe 1f
    }

    @Test
    fun `contrast goes from 1 for the same color to 21 for black on white`() {
        contrast(Color.White, Color.White) shouldBe 1f
        contrast(Color.Black, Color.White) shouldBe (21f plusOrMinus 0.001f)
    }

    private companion object {
        const val MIN_CONTRAST = 3f
        const val GREYISH = 0.15f

        // The ink is made at 0.6; storing it as 8-bit sRGB rounds a little of that away.
        const val VIVID = 0.58f
        const val HUE_TOLERANCE = 2f
        const val HUE_STEP = 15
        val PALE_GREY = Color(0xFFC8C8C8)
    }
}
