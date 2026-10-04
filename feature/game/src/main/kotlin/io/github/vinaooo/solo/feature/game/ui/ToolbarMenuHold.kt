package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.delay

/** True while the menu is open, and until the buttons have grown back after it closes. */
@Composable
internal fun rememberMenuHold(menuOpen: Boolean): Boolean {
    var hold by remember { mutableStateOf(false) }
    LaunchedEffect(menuOpen) {
        if (menuOpen) {
            hold = true
        } else {
            delay(MENU_SETTLE_MILLIS)
            hold = false
        }
    }
    return hold || menuOpen
}

/**
 * While [hold], the toolbar keeps the room it had at its largest and sits at its end (right, or bottom when
 * [vertical]), so as its buttons leave, the menu's close button at that end stays where it was instead of drifting
 * to the middle. Otherwise it takes its own size.
 */
internal fun Modifier.keepingEnd(vertical: Boolean, hold: Boolean): Modifier = composed {
    val largest = remember { IntArray(1) }
    layout { measurable, constraints ->
        val toolbar = measurable.measure(constraints)
        val length = if (vertical) toolbar.height else toolbar.width
        largest[0] = if (hold) maxOf(largest[0], length) else length
        val room = largest[0]
        if (vertical) {
            layout(toolbar.width, room) { toolbar.placeRelative(0, room - toolbar.height) }
        } else {
            layout(room, toolbar.height) { toolbar.placeRelative(room - toolbar.width, 0) }
        }
    }
}

/** Long enough for the toolbar's buttons to grow back after the menu closes. */
private const val MENU_SETTLE_MILLIS = 700L
