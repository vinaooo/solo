package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
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
        // Surface stretches each direct child to its full size, so the tiny announcer sits in a Box of its own.
        Box {
            GameContent(uiState, onIntent, onOpenScores, onOpenSettings, snackbar)
            Announcer(uiState.announcement)
        }
    }
    uiState.winRecord?.let { WinDialog(it, onNewGame = { onIntent(GameIntent.NewGame) }) }
}

/**
 * Portrait: stats on top, board below, toolbar floating at the bottom.
 * Landscape: stats, board and toolbar side by side, the board centered.
 */
@Composable
private fun GameContent(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbar: SnackbarHostState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            LandscapeGame(uiState, onIntent, onOpenScores, onOpenSettings)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        } else {
            PortraitGame(uiState, onIntent, onOpenScores, onOpenSettings)
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = TOOLBAR_SPACE))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PortraitGame(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(uiState, onOpenScores, onOpenSettings)
            BoardOrLoading(
                uiState,
                onIntent,
                Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp, bottom = TOOLBAR_SPACE),
            )
        }
        HorizontalGameToolbar(
            uiState = uiState,
            onIntent = onIntent,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = -FloatingToolbarDefaults.ScreenOffset),
        )
    }
}

/** The board centered at full height, info on its left and the game actions in a vertical toolbar on its right. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LandscapeGame(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    CenteredRow(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
        start = { GameSidePanel(uiState, onOpenScores, onOpenSettings, Modifier.fillMaxHeight()) },
        center = { BoardOrLoading(uiState, onIntent, Modifier.fillMaxSize().padding(vertical = 8.dp)) },
        end = {
            VerticalGameToolbar(
                uiState = uiState,
                onIntent = onIntent,
                modifier = Modifier.padding(end = FloatingToolbarDefaults.ScreenOffset),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoardOrLoading(uiState: GameUiState, onIntent: (GameIntent) -> Unit, modifier: Modifier) {
    val session = uiState.session
    if (session == null) {
        Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
    } else {
        GameBoard(
            state = session.state,
            hint = uiState.hint,
            onIntent = onIntent,
            modifier = modifier,
            destinations = uiState.destinations,
            handedness = uiState.settings.handedness,
        )
    }
}

private val TOOLBAR_SPACE = 88.dp
