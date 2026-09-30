package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Fallback palette (green felt seed) for devices without dynamic color, or when the player turns it off. */
object BrandColors {
    val light = lightColorScheme(
        primary = Color(0xFF1E6A4F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFA8F2CF),
        onPrimaryContainer = Color(0xFF002115),
        inversePrimary = Color(0xFF8CD5B4),
        secondary = Color(0xFF4C6358),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCEE9DA),
        onSecondaryContainer = Color(0xFF092017),
        tertiary = Color(0xFF3E6374),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC2E8FC),
        onTertiaryContainer = Color(0xFF001F2A),
        background = Color(0xFFF5FBF6),
        onBackground = Color(0xFF171D1A),
        surface = Color(0xFFF5FBF6),
        onSurface = Color(0xFF171D1A),
        surfaceVariant = Color(0xFFDBE5DE),
        onSurfaceVariant = Color(0xFF404944),
        inverseSurface = Color(0xFF2C322F),
        inverseOnSurface = Color(0xFFECF2ED),
        outline = Color(0xFF707973),
        outlineVariant = Color(0xFFBFC9C2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEFF5F0),
        surfaceContainer = Color(0xFFE9EFEA),
        surfaceContainerHigh = Color(0xFFE4EAE5),
        surfaceContainerHighest = Color(0xFFDEE4DF),
    )

    val dark = darkColorScheme(
        primary = Color(0xFF8CD5B4),
        onPrimary = Color(0xFF003826),
        primaryContainer = Color(0xFF005139),
        onPrimaryContainer = Color(0xFFA8F2CF),
        inversePrimary = Color(0xFF1E6A4F),
        secondary = Color(0xFFB3CCBF),
        onSecondary = Color(0xFF1E352B),
        secondaryContainer = Color(0xFF354B41),
        onSecondaryContainer = Color(0xFFCEE9DA),
        tertiary = Color(0xFFA6CCDF),
        onTertiary = Color(0xFF0A3444),
        tertiaryContainer = Color(0xFF254B5C),
        onTertiaryContainer = Color(0xFFC2E8FC),
        background = Color(0xFF0F1512),
        onBackground = Color(0xFFDEE4DF),
        surface = Color(0xFF0F1512),
        onSurface = Color(0xFFDEE4DF),
        surfaceVariant = Color(0xFF404944),
        onSurfaceVariant = Color(0xFFBFC9C2),
        inverseSurface = Color(0xFFDEE4DF),
        inverseOnSurface = Color(0xFF2C322F),
        outline = Color(0xFF89938D),
        outlineVariant = Color(0xFF404944),
        surfaceContainerLowest = Color(0xFF0A0F0D),
        surfaceContainerLow = Color(0xFF171D1A),
        surfaceContainer = Color(0xFF1B211E),
        surfaceContainerHigh = Color(0xFF252B28),
        surfaceContainerHighest = Color(0xFF303633),
    )
}
