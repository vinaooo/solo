package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.github.vinaooo.vinkit.designsystem.isDarkTheme

/** vinkit's theme (green is Solo's brand), with the card colors drawn from its scheme. */
@Composable
fun SoloTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeColor: ThemeColor = ThemeColor.GREEN,
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeMode, isSystemInDarkTheme())
    VinkitTheme(themeColor, themeMode, dynamicColor, SoloTypography) {
        val colorScheme = MaterialTheme.colorScheme
        val cardColors = remember(colorScheme, darkTheme) { cardColorsFor(colorScheme, darkTheme) }
        CompositionLocalProvider(LocalCardColors provides cardColors, content = content)
    }
}

object SoloThemeExtras {
    val cardColors: CardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCardColors.current

    val motion: MotionScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.motionScheme
}
