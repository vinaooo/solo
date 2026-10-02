package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.ui.formatElapsed
import io.github.vinaooo.solo.core.ui.spokenElapsed
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.R

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
        Stats(uiState)
        Spacer(Modifier.weight(1f))
        Row { NavigationButtons(onOpenScores, onOpenSettings) }
    }
}

@Composable
private fun Stats(uiState: GameUiState) {
    val state = uiState.session?.state
    Stat(stringResource(R.string.score), (state?.score ?: 0).toString())
    Stat(stringResource(R.string.moves), (state?.moves ?: 0).toString())
    if (uiState.settings.showTimer) {
        val elapsed = state?.elapsedSeconds ?: 0
        Stat(stringResource(R.string.time), formatElapsed(elapsed), spoken = spokenElapsed(elapsed))
    }
}

/** A label over its value, read by TalkBack as one item: "Score, 25". */
@Composable
private fun Stat(label: String, value: String, spoken: String = value) {
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $spoken" }) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) { ToolbarActions(uiState, onIntent, vertical = false) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun VerticalGameToolbar(uiState: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    VerticalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) { ToolbarActions(uiState, onIntent, vertical = true) }
}

/** Undo, redo, hint, auto-complete (when possible) and the new game menu, in either toolbar. */
@Composable
private fun ToolbarActions(uiState: GameUiState, onIntent: (GameIntent) -> Unit, vertical: Boolean) {
    IconButton(onClick = { onIntent(GameIntent.Undo) }, enabled = uiState.session?.canUndo == true) {
        Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo))
    }
    IconButton(onClick = { onIntent(GameIntent.Redo) }, enabled = uiState.session?.canRedo == true) {
        Icon(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.redo))
    }
    IconButton(onClick = { onIntent(GameIntent.Hint) }) {
        Icon(Icons.Rounded.Lightbulb, stringResource(R.string.hint))
    }
    // The button grows out of its slot, widening the toolbar (lengthening it in landscape), and scales up into place.
    // Only along the toolbar: growing across it too leaves the toolbar's balanced padding, and so its thickness, wrong.
    val size = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    AnimatedVisibility(
        visible = uiState.canAutoComplete,
        enter = if (vertical) {
            expandVertically(size, Alignment.CenterVertically)
        } else {
            expandHorizontally(size, Alignment.CenterHorizontally)
        } + scaleIn(scale) + fadeIn(fade),
        exit = if (vertical) {
            shrinkVertically(size, Alignment.CenterVertically)
        } else {
            shrinkHorizontally(size, Alignment.CenterHorizontally)
        } + scaleOut(scale) + fadeOut(fade),
    ) {
        IconButton(onClick = { onIntent(GameIntent.AutoComplete) }, enabled = !uiState.isAutoCompleting) {
            Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.auto_complete))
        }
    }
    NewGameMenu(onIntent)
}

@Composable
private fun NewGameMenu(onIntent: (GameIntent) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.Style, stringResource(R.string.new_game)) }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_game)) },
                leadingIcon = { Icon(Icons.Rounded.Style, null) },
                onClick = {
                    menuOpen = false
                    onIntent(GameIntent.NewGame)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.restart_deal)) },
                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                onClick = {
                    menuOpen = false
                    onIntent(GameIntent.RestartDeal)
                },
            )
        }
    }
}
