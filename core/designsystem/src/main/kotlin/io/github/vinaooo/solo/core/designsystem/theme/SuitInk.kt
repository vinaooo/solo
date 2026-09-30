package io.github.vinaooo.solo.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.sqrt

/**
 * The ink for hearts and diamonds: the theme [accent]'s hue, made vivid and moved to the lightness that contrasts
 * equally with the [neutral] ink of spades and clubs and with the card [face]. The accent itself is usually close
 * to the neutral ink in lightness (both are pale in dark themes), so as it is the two suit colors would blur.
 * A grey accent (a grey wallpaper under dynamic color) stays grey instead of getting a hue it doesn't have.
 */
internal fun accentSuitInk(accent: Color, neutral: Color, face: Color): Color {
    val saturation = accent.saturation()
    val vivid = if (saturation < MIN_ACCENT_SATURATION) {
        accent
    } else {
        Color.hsv(accent.hue(), maxOf(saturation, VIVID_SATURATION), maxOf(accent.red, accent.green, accent.blue))
    }
    val (lighter, darker) = listOf(neutral.luminance(), face.luminance()).sortedDescending()
    // Equal contrast with both: (lighter + k) / (target + k) == (target + k) / (darker + k).
    val target = sqrt((lighter + LUMINANCE_OFFSET) * (darker + LUMINANCE_OFFSET)) - LUMINANCE_OFFSET
    return withLuminance(vivid, target)
}

/** WCAG contrast ratio between two colors, from 1 (same) to 21 (black on white). */
internal fun contrast(a: Color, b: Color): Float {
    val (lighter, darker) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}

/** HSV saturation: 0 for greys, 1 for fully saturated colors. */
internal fun Color.saturation(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    return if (max == 0f) 0f else (max - min) / max
}

/** HSV hue in degrees, from 0 up to 360; 0 for greys. */
internal fun Color.hue(): Float {
    val max = maxOf(red, green, blue)
    val delta = max - minOf(red, green, blue)
    if (delta == 0f) return 0f
    val sector = when (max) {
        red -> (green - blue) / delta
        green -> (blue - red) / delta + GREEN_SECTOR
        else -> (red - green) / delta + BLUE_SECTOR
    }
    return (sector * DEGREES_PER_SECTOR + FULL_TURN) % FULL_TURN
}

/**
 * [color] blended toward black or white until its luminance reaches [target]. It blends the RGB channels
 * directly (Compose's lerp blends in Oklab, which shifts the hue), so the hue stays the same.
 */
private fun withLuminance(color: Color, target: Float): Color {
    val anchor = if (color.luminance() > target) Color.Black else Color.White
    var low = 0f
    var high = 1f
    repeat(SEARCH_STEPS) {
        val mid = (low + high) / 2
        val luminance = blend(color, anchor, mid).luminance()
        val tooFar = if (anchor == Color.Black) luminance < target else luminance > target
        if (tooFar) high = mid else low = mid
    }
    return blend(color, anchor, low)
}

private fun blend(from: Color, to: Color, fraction: Float) = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
)

private const val MIN_ACCENT_SATURATION = 0.15f
private const val VIVID_SATURATION = 0.6f
private const val DEGREES_PER_SECTOR = 60f

// The hue wheel in 60° sectors: red starts at 0, green at 2 (120°), blue at 4 (240°).
private const val GREEN_SECTOR = 2f
private const val BLUE_SECTOR = 4f
private const val FULL_TURN = 360f
private const val LUMINANCE_OFFSET = 0.05f
private const val SEARCH_STEPS = 24
