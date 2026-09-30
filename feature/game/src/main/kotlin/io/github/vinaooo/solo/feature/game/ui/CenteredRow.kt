package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints

/**
 * [start], [center] and [end] side by side, with both sides as wide as the wider of the two, so [center] is
 * centered on the screen whatever the sides hold. Sides are vertically centered, [start] hugging the leading
 * edge and [end] the trailing one; [center] gets all the height.
 */
@Composable
internal fun CenteredRow(
    start: @Composable () -> Unit,
    center: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(start, center, end), modifier = modifier) { (starts, centers, ends), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val startPlaceables = starts.map { it.measure(loose) }
        val endPlaceables = ends.map { it.measure(loose) }
        val side = (startPlaceables + endPlaceables).maxOfOrNull { it.width } ?: 0
        val centerWidth = (constraints.maxWidth - 2 * side).coerceAtLeast(0)
        val centerPlaceables = centers.map {
            it.measure(Constraints.fixed(centerWidth, constraints.maxHeight))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            startPlaceables.forEach { it.placeRelative(0, (constraints.maxHeight - it.height) / 2) }
            centerPlaceables.forEach { it.placeRelative(side, 0) }
            endPlaceables.forEach {
                it.placeRelative(constraints.maxWidth - it.width, (constraints.maxHeight - it.height) / 2)
            }
        }
    }
}
