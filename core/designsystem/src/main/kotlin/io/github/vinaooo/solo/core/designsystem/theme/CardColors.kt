package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for the playing table. Suits stay classic red/black; the back and highlights follow the theme. */
@Immutable
data class CardColors(
    val table: Color,
    val face: Color,
    val red: Color,
    val black: Color,
    val border: Color,
    val back: Color,
    val backPattern: Color,
    val highlight: Color,
    val emptySlot: Color,
)

internal fun cardColorsFor(scheme: ColorScheme, darkTheme: Boolean) = CardColors(
    table = scheme.surfaceContainer,
    face = if (darkTheme) FaceDark else FaceLight,
    red = if (darkTheme) RedDark else RedLight,
    black = if (darkTheme) BlackDark else BlackLight,
    border = scheme.outlineVariant,
    back = scheme.primary,
    backPattern = scheme.primaryContainer,
    highlight = scheme.tertiary,
    emptySlot = scheme.outline,
)

// Suit inks keep at least 4.5:1 contrast on their card face in both themes.
private val FaceLight = Color(0xFFFFFFFF)
private val FaceDark = Color(0xFF2B302D)
private val RedLight = Color(0xFFC62828)
private val RedDark = Color(0xFFFF8A80)
private val BlackLight = Color(0xFF1B1C1E)
private val BlackDark = Color(0xFFE8ECE9)

val LocalCardColors = staticCompositionLocalOf { cardColorsFor(BrandColors.light, darkTheme = false) }
