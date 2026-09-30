package io.github.vinaooo.solo.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape

object CardDimensions {
    /** Width / height of a playing card (poker size, 63 × 88 mm). */
    const val ASPECT_RATIO = 63f / 88f

    val shape = RoundedCornerShape(percent = 10)
}
