package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors for the playing table. Spades and clubs take the color of the game toolbar's icons (onPrimaryContainer):
 * pale on the dark faces, dark on the light ones. Hearts and diamonds take a vivid shade of the theme's accent
 * that stands apart from both (see [accentSuitInk]). The back and highlights follow the theme too.
 */
@Immutable
data class CardColors(
    val table: Color,
    val face: Color,
    val redSuits: Color,
    val blackSuits: Color,
    val border: Color,
    val back: Color,
    val backPattern: Color,
    val highlight: Color,
    val emptySlot: Color,
)

internal fun cardColorsFor(scheme: ColorScheme, darkTheme: Boolean): CardColors {
    val face = if (darkTheme) FaceDark else FaceLight
    val blackSuits = scheme.onPrimaryContainer
    return CardColors(
        table = scheme.surfaceContainer,
        face = face,
        redSuits = accentSuitInk(scheme.primary, neutral = blackSuits, face = face),
        blackSuits = blackSuits,
        border = scheme.outlineVariant,
        back = scheme.primary,
        backPattern = scheme.primaryContainer,
        highlight = scheme.tertiary,
        emptySlot = scheme.outline,
    )
}

private val FaceLight = Color(0xFFFFFFFF)
private val FaceDark = Color(0xFF2B302D)

val LocalCardColors = staticCompositionLocalOf { cardColorsFor(BrandColors.light, darkTheme = false) }
