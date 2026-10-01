package io.github.vinaooo.solo.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras

/** Card-sized slot where a pile is empty; a null [borderWidth] hides its outline. */
@Composable
fun EmptyPileSlot(modifier: Modifier = Modifier, icon: ImageVector? = null, borderWidth: Dp? = 2.dp) {
    val colors = SoloThemeExtras.cardColors
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(CardDimensions.ASPECT_RATIO)
            .then(
                if (borderWidth == null) {
                    Modifier
                } else {
                    Modifier.border(borderWidth, colors.emptySlot.copy(alpha = 0.5f), CardDimensions.shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, Modifier.size(maxWidth * 0.6f), colors.emptySlot.copy(alpha = 0.6f))
        }
    }
}
