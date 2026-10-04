package io.github.vinaooo.solo.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.ui.label
import io.github.vinaooo.solo.domain.model.GameMode

/**
 * The scoring mode, as an Expressive connected button group: one icon per mode, the chosen one a filled pill, and
 * under the group the chosen mode's name and what it means. Four long names wouldn't fit a row on a phone; the icons
 * do, and TalkBack reads each one's name.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameModeChoice(selected: GameMode, onSelect: (GameMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.game_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth().selectableGroup(),
        ) {
            GameMode.entries.forEachIndexed { index, mode ->
                val bounce = rememberBounce(mode == selected)
                ToggleButton(
                    checked = mode == selected,
                    onCheckedChange = { onSelect(mode) },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        GameMode.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(BUTTON_HEIGHT)
                        .graphicsLayer {
                            scaleX = bounce.value
                            scaleY = bounce.value
                        }
                        .semantics { role = Role.RadioButton },
                ) { Icon(icons.getValue(mode), stringResource(mode.label)) }
            }
        }
        // The new mode's name and meaning bounce in from below as the old ones fade.
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                (slideInVertically(bouncy()) { it / 2 } + fadeIn()) togetherWith fadeOut()
            },
            label = "mode description",
        ) { mode ->
            Column {
                Text(
                    stringResource(mode.label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    stringResource(descriptions.getValue(mode)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A button's scale: when it becomes [checked] it squashes, then springs back past its size and settles, a bounce.
 * Not on first showing: only a change bounces.
 */
@Composable
private fun rememberBounce(checked: Boolean): Animatable<Float, AnimationVector1D> {
    val scale = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (checked && shown) {
            scale.snapTo(SQUASH)
            scale.animateTo(1f, bouncy())
        }
        shown = true
    }
    return scale
}

private fun <T> bouncy() = spring<T>(dampingRatio = BOUNCE_DAMPING, stiffness = Spring.StiffnessMediumLow)

private const val SQUASH = 0.85f
private const val BOUNCE_DAMPING = 0.4f

private val icons = mapOf(
    GameMode.STANDARD to Icons.Rounded.Star,
    GameMode.VEGAS to Icons.Rounded.AttachMoney,
    GameMode.VEGAS_CUMULATIVE to Icons.Rounded.Savings,
    GameMode.COUNTER_TIME to Icons.Rounded.Timer,
)

private val descriptions = mapOf(
    GameMode.STANDARD to R.string.mode_standard_note,
    GameMode.VEGAS to R.string.mode_vegas_note,
    GameMode.VEGAS_CUMULATIVE to R.string.mode_vegas_cumulative_note,
    GameMode.COUNTER_TIME to R.string.mode_counter_time_note,
)

private val BUTTON_HEIGHT = 56.dp
