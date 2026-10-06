package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.ui.formatDollars
import io.github.vinaooo.solo.core.ui.formatElapsed
import io.github.vinaooo.solo.core.ui.spokenDollars
import io.github.vinaooo.solo.core.ui.spokenElapsed
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.R
import kotlinx.coroutines.withTimeoutOrNull

/** Portrait: stats in a row, the Scores and Settings buttons at its end. */
@Composable
internal fun GameTopBar(uiState: GameUiState, onOpenScores: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Stats(uiState)
        Spacer(Modifier.weight(1f))
        NavigationButtons(onOpenScores, onOpenSettings)
    }
}

/** Landscape: stats stacked on the left of the board, the Scores and Settings buttons below them. */
@Composable
internal fun GameSidePanel(
    uiState: GameUiState,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Stats(uiState, large = true)
        Spacer(Modifier.weight(1f))
        Row { NavigationButtons(onOpenScores, onOpenSettings) }
    }
}

@Composable
/**
 * [large] in landscape's side panel, which has the room for it. Vegas scores in dollars; counter time ranks by time
 * alone, so it shows the time left instead of points. Only Standard shows the time played; Vegas doesn't score it.
 */
private fun Stats(uiState: GameUiState, large: Boolean = false) {
    val state = uiState.session?.state
    val score = state?.score ?: 0
    val mode = state?.mode ?: uiState.settings.gameMode
    when {
        mode.isVegas -> Stat(stringResource(R.string.score), formatDollars(score), large, spoken = spokenDollars(score))
        mode != GameMode.COUNTER_TIME -> Stat(stringResource(R.string.score), score.toString(), large)
    }
    Stat(stringResource(R.string.moves), (state?.moves ?: 0).toString(), large)
    val left = state?.secondsLeft
    if (left != null) {
        Stat(stringResource(R.string.time_left), formatElapsed(left), large, spoken = spokenElapsed(left))
    } else if (mode == GameMode.STANDARD) {
        val elapsed = state?.elapsedSeconds ?: 0
        Stat(stringResource(R.string.time), formatElapsed(elapsed), large, spoken = spokenElapsed(elapsed))
    }
}

/** A label over its value, read by TalkBack as one item: "Score, 25". */
@Composable
private fun Stat(label: String, value: String, large: Boolean, spoken: String = value) {
    val typography = MaterialTheme.typography
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $spoken" }) {
        Text(
            label,
            style = if (large) typography.titleSmall else typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (large) typography.headlineMedium else typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NavigationButtons(onOpenScores: () -> Unit, onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenScores) { Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.open_scores)) }
    IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.open_settings)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HorizontalGameToolbar(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dim by animateFloatAsState(
        if (menuOpen) MENU_SCRIM_ALPHA else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier.scrimBehind(MaterialTheme.colorScheme.scrim) { dim }
            .keepingEnd(vertical = false, hold = rememberMenuHold(menuOpen)),
    ) { ToolbarActions(uiState, onIntent, vertical = false, menuOpen) { menuOpen = it } }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun VerticalGameToolbar(uiState: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    val dim by animateFloatAsState(
        if (menuOpen) MENU_SCRIM_ALPHA else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    VerticalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier.scrimBehind(MaterialTheme.colorScheme.scrim) { dim }
            .keepingEnd(vertical = true, hold = rememberMenuHold(menuOpen)),
    ) { ToolbarActions(uiState, onIntent, vertical = true, menuOpen) { menuOpen = it } }
}

/**
 * Undo, redo, hint, auto-complete (when possible) and the new game menu, in either toolbar. The toolbar owns whether
 * the menu is open, because it dims the game behind itself meanwhile.
 */
@Composable
private fun ToolbarActions(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    vertical: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
) {
    // A button grows out of its slot, widening the toolbar (lengthening it in landscape), and scales up into place;
    // it leaves the same way. Only along the toolbar: growing across it too leaves the toolbar's balanced padding, and
    // so its thickness, wrong. While the menu is open every button but its close button leaves, so the toolbar shrinks
    // to that one button, and grows back when the menu closes.
    val size = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val enter = if (vertical) {
        expandVertically(size, Alignment.CenterVertically)
    } else {
        expandHorizontally(size, Alignment.CenterHorizontally)
    } + scaleIn(scale) + fadeIn(fade)
    val exit = if (vertical) {
        shrinkVertically(size, Alignment.CenterVertically)
    } else {
        shrinkHorizontally(size, Alignment.CenterHorizontally)
    } + scaleOut(scale) + fadeOut(fade)
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Undo) }, enabled = uiState.session?.canUndo == true) {
            Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo))
        }
    }
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Redo) }, enabled = uiState.session?.canRedo == true) {
            Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.redo))
        }
    }
    AnimatedVisibility(!menuOpen, enter = enter, exit = exit) {
        IconButton(onClick = { onIntent(GameIntent.Hint) }) {
            Icon(Icons.Rounded.Lightbulb, stringResource(R.string.hint))
        }
    }
    AnimatedVisibility(uiState.canAutoComplete && !menuOpen, enter = enter, exit = exit) {
        // The first few times the button appears, a bubble points it out.
        TipBox(
            show = uiState.showAutoCompleteTip,
            text = stringResource(R.string.auto_complete_tip),
            icon = Icons.Rounded.AutoAwesome,
            vertical = vertical,
            onShown = { onIntent(GameIntent.AutoCompleteTipShown) },
        ) {
            IconButton(onClick = { onIntent(GameIntent.AutoComplete) }, enabled = !uiState.isAutoCompleting) {
                Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.auto_complete))
            }
        }
    }
    NewGameMenu(menuOpen, onMenuOpenChange, onIntent, vertical)
}

/**
 * A toolbar button ([content]) that, when [show] turns true, gets a speech bubble pointing at it (from above, or from
 * the left of the vertical toolbar) until the player taps anywhere or a few seconds pass; then [onShown].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TipBox(
    show: Boolean,
    text: String,
    icon: ImageVector,
    vertical: Boolean,
    onShown: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tip = rememberTooltipState(isPersistent = true)
    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        try {
            withTimeoutOrNull(TIP_MILLIS) { tip.show() }
        } finally {
            tip.dismiss()
            onShown()
        }
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            if (vertical) TooltipAnchorPosition.Left else TooltipAnchorPosition.Above,
            // Clears the toolbar's own padding around the button, so the bubble floats just off the toolbar.
            spacingBetweenTooltipAndAnchor = 16.dp,
        ),
        tooltip = {
            // Expressive: a pill in the accent (tertiary) color, with the button's icon and emphasized text.
            PlainTooltip(
                caretShape = TooltipDefaults.caretShape(),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text, style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        },
        state = tip,
        content = content,
    )
}

private const val TIP_MILLIS = 5_000L
