package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.core.ui.formatElapsed
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameMessage
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.GameViewModel
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.solo.feature.game.board.GameBoard

@Composable
fun GameRoute(
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(GameIntent.Resume)
        onPauseOrDispose { viewModel.onIntent(GameIntent.Pause) }
    }
    GameScreen(uiState, viewModel::onIntent, onOpenScores, onOpenSettings, modifier)
}

@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val noMoves = stringResource(R.string.no_moves)
    LaunchedEffect(uiState.message) {
        if (uiState.message == GameMessage.NO_MOVES) {
            snackbar.showSnackbar(noMoves)
            onIntent(GameIntent.MessageShown)
        }
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SoloThemeExtras.cardColors.table,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        GameContent(uiState, onIntent, onOpenScores, onOpenSettings, snackbar)
    }
    uiState.winRecord?.let { WinDialog(it, onNewGame = { onIntent(GameIntent.NewGame) }) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameContent(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbar: SnackbarHostState,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(uiState, onOpenScores, onOpenSettings)
            val session = uiState.session
            if (session == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            } else {
                GameBoard(
                    state = session.state,
                    hint = uiState.hint,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = TOOLBAR_SPACE),
                )
            }
        }
        GameToolbar(
            uiState = uiState,
            onIntent = onIntent,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -FloatingToolbarDefaults.ScreenOffset),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = TOOLBAR_SPACE))
    }
}

@Composable
private fun GameTopBar(uiState: GameUiState, onOpenScores: () -> Unit, onOpenSettings: () -> Unit) {
    val state = uiState.session?.state
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Stat(stringResource(R.string.score), (state?.score ?: 0).toString())
        Stat(stringResource(R.string.moves), (state?.moves ?: 0).toString())
        if (uiState.settings.showTimer) Stat(stringResource(R.string.time), formatElapsed(state?.elapsedSeconds ?: 0))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenScores) { Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.open_scores)) }
        IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.open_settings)) }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameToolbar(uiState: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) {
        IconButton(onClick = { onIntent(GameIntent.Undo) }, enabled = uiState.session?.canUndo == true) {
            Icon(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.undo))
        }
        IconButton(onClick = { onIntent(GameIntent.Hint) }) {
            Icon(Icons.Rounded.Lightbulb, stringResource(R.string.hint))
        }
        if (uiState.canAutoComplete) {
            IconButton(onClick = { onIntent(GameIntent.AutoComplete) }, enabled = !uiState.isAutoCompleting) {
                Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.auto_complete))
            }
        }
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
}

@Composable
private fun WinDialog(record: ScoreRecord, onNewGame: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.you_won)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.win_score, record.points))
                Text(stringResource(R.string.win_time, formatElapsed(record.elapsedSeconds)))
                Text(stringResource(R.string.win_moves, record.moves))
            }
        },
        confirmButton = { TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) } },
    )
}

private val TOOLBAR_SPACE = 88.dp
