package io.github.vinaooo.solo.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape

object CardDimensions {
    /** Width / height of a playing card (poker size, 63 × 88 mm). */
    const val ASPECT_RATIO = 63f / 88f

    /** Corner radius, as a percentage of the card's width. */
    const val CORNER_PERCENT = 10

    val shape = RoundedCornerShape(percent = CORNER_PERCENT)
}
