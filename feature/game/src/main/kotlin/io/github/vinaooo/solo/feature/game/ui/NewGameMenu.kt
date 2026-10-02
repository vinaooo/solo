package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * New game or restart, as an Expressive FAB menu: the options shoot up out of the button one after another, each
 * overshooting with a bounce (to the left of the vertical toolbar), and the button turns into a close button meanwhile.
 * The pills follow the FAB menu spec, drawn here because its own column clips them and only widens them in place.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NewGameMenu(
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onIntent: (GameIntent) -> Unit,
    vertical: Boolean,
) {
    val options = listOf(
        Triple(Icons.Rounded.Style, R.string.new_game, GameIntent.NewGame),
        Triple(Icons.Rounded.Refresh, R.string.restart_deal, GameIntent.RestartDeal),
    )
    // 0 = tucked into the button, 1 = in place. The pill nearest the button leaves first and comes back last.
    val progress = remember { options.map { Animatable(0f) } }
    val leave = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(menuOpen) {
        progress.forEachIndexed { index, pill ->
            val fromButton = progress.lastIndex - index
            launch {
                delay(MENU_STAGGER_MILLIS * if (menuOpen) fromButton else index)
                pill.animateTo(if (menuOpen) 1f else 0f, if (menuOpen) MENU_BOUNCE else leave)
            }
        }
    }
    Box {
        IconButton(onClick = { onMenuOpenChange(!menuOpen) }) {
            Crossfade(menuOpen, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "menu icon") {
                if (it) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.close_menu))
                } else {
                    Icon(Icons.Rounded.Style, stringResource(R.string.new_game))
                }
            }
        }
        if (menuOpen || progress.any { it.value != 0f }) {
            Popup(
                popupPositionProvider = with(LocalDensity.current) {
                    MenuBesideAnchor(vertical, gap = 16.dp.roundToPx(), toolbarInset = 8.dp.roundToPx())
                },
                onDismissRequest = { onMenuOpenChange(false) },
                properties = PopupProperties(focusable = menuOpen),
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEachIndexed { index, (icon, label, intent) ->
                        MenuPill(
                            icon,
                            stringResource(label),
                            progress[index]::value,
                            options.lastIndex - index,
                            vertical,
                        ) {
                            onMenuOpenChange(false)
                            onIntent(intent)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One option of the new game menu: a FAB menu pill that sits [fromButton] places from the button and, as [progress]
 * goes from 0 to 1, grows out of it (from below, or from the right when [vertical]).
 */
@Composable
private fun MenuPill(
    icon: ImageVector,
    label: String,
    progress: () -> Float,
    fromButton: Int,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.height(56.dp).graphicsLayer {
            val away = 1f - progress()
            translationY = away * (fromButton + if (vertical) 0 else 1) * size.height * 1.1f
            translationX = if (vertical) away * size.height * 1.3f else 0f
            scaleX = 0.5f + 0.5f * progress()
            scaleY = scaleX
            alpha = progress().coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(1f, 1f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Places the menu above the button, its right edge on the toolbar's, or to its left when [beside], its bottom on the
 * toolbar's. [gap] clears the toolbar and [toolbarInset] is the toolbar's padding around the button.
 */
private data class MenuBesideAnchor(private val beside: Boolean, private val gap: Int, private val toolbarInset: Int) :
    PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = if (beside) {
        IntOffset(
            anchorBounds.left - gap - popupContentSize.width,
            anchorBounds.bottom + toolbarInset - popupContentSize.height,
        )
    } else {
        IntOffset(
            anchorBounds.right + toolbarInset - popupContentSize.width,
            anchorBounds.top - gap - popupContentSize.height,
        )
    }
}

/** A clearly bouncy spring, so each pill overshoots and settles like Keep's. */
private val MENU_BOUNCE = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)
private const val MENU_STAGGER_MILLIS = 60L

/**
 * Draws a [color] scrim, [alpha] opaque, over everything drawn before this element: the whole game, when it modifies
 * the toolbar, which comes last. The toolbar itself draws on top, so it stays undimmed.
 */
internal fun Modifier.scrimBehind(color: Color, alpha: () -> Float): Modifier = drawBehind {
    val a = alpha()
    // ponytail: a rectangle far larger than any screen instead of measuring the window; the root clips it anyway.
    if (a >
        0f
    ) {
        drawRect(
            color.copy(alpha = a),
            topLeft = Offset(-SCRIM_REACH, -SCRIM_REACH),
            size = Size(
                2 * SCRIM_REACH,
                2 * SCRIM_REACH,
            ),
        )
    }
}

/** Stronger than Material's standard 0.32 scrim, so the menu stands out clearly from the game. */
internal const val MENU_SCRIM_ALPHA = 0.6f
private const val SCRIM_REACH = 100_000f
