package io.github.vinaooo.solo.core.designsystem.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.vinaooo.solo.domain.model.ThemeMode

fun isDarkTheme(themeMode: ThemeMode, systemInDark: Boolean): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Dynamic (wallpaper) colors on Android 12+, brand colors otherwise. */
fun colorSchemeFor(context: Context, darkTheme: Boolean, dynamicColor: Boolean): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    darkTheme -> BrandColors.dark
    else -> BrandColors.light
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoloTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val darkTheme = isDarkTheme(themeMode, isSystemInDarkTheme())
    val context = LocalContext.current
    val colorScheme = remember(context, darkTheme, dynamicColor) { colorSchemeFor(context, darkTheme, dynamicColor) }
    val cardColors = remember(colorScheme, darkTheme) { cardColorsFor(colorScheme, darkTheme) }
    CompositionLocalProvider(LocalCardColors provides cardColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = SoloTypography,
            content = content,
        )
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
