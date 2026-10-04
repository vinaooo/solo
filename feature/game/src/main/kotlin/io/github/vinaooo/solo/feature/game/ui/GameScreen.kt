package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.domain.model.PhoneViewSide
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameMessage
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.GameViewModel
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.solo.feature.game.board.GameBoard
import kotlinx.coroutines.delay

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
        // While the stuck message shows, any tap anywhere dismisses it, and still reaches the game.
        Box(
            Modifier.pointerInput(uiState.showStuckTip) {
                if (!uiState.showStuckTip) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    onIntent(GameIntent.StuckTipShown)
                }
            },
        ) {
            GameContent(uiState, onIntent, onOpenScores, onOpenSettings, snackbar)
            StuckMessage(uiState.showStuckTip, { onIntent(GameIntent.StuckTipShown) }, Modifier.align(Alignment.Center))
            Announcer(uiState.announcement)
        }
    }
    uiState.winRecord?.let { WinDialog(it, onNewGame = { onIntent(GameIntent.NewGame) }) }
}

/**
 * "No moves left", in the middle of the screen: an Expressive pill that springs in when the game gets stuck and goes
 * away after a few seconds (or on any tap, handled by the caller). What to do about it is up to the player.
 */
@Composable
private fun StuckMessage(show: Boolean, onShown: () -> Unit, modifier: Modifier) {
    LaunchedEffect(show) {
        if (show) {
            delay(STUCK_MESSAGE_MILLIS)
            onShown()
        }
    }
    val spring = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    AnimatedVisibility(
        visible = show,
        modifier = modifier.padding(horizontal = 32.dp),
        enter = scaleIn(spring) + fadeIn(fade),
        exit = scaleOut(spring) + fadeOut(fade),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shadowElevation = 3.dp,
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.stuck_title), style = MaterialTheme.typography.titleLargeEmphasized)
                Text(
                    stringResource(R.string.stuck_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
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
        center = {
            BoardOrLoading(uiState, onIntent, Modifier.fillMaxSize().padding(vertical = 8.dp), sideways = true)
        },
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
private fun BoardOrLoading(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier,
    sideways: Boolean = false,
) {
    val session = uiState.session
    if (session == null) {
        Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
    } else {
        // Phone view on a tablet: the traditional board, as wide as a phone's, at the top of the room it has, on the
        // side the player chose.
        val phoneView = uiState.settings.phoneView &&
            LocalConfiguration.current.smallestScreenWidthDp >= TABLET_WIDTH_DP
        val side = when (uiState.settings.phoneViewSide) {
            PhoneViewSide.LEFT -> Alignment.TopStart
            PhoneViewSide.CENTER -> Alignment.TopCenter
            PhoneViewSide.RIGHT -> Alignment.TopEnd
        }
        Box(modifier, contentAlignment = side) {
            GameBoard(
                state = session.state,
                hint = uiState.hint,
                onIntent = onIntent,
                modifier = (if (phoneView) Modifier.widthIn(max = PHONE_WIDTH) else Modifier).fillMaxSize(),
                destinations = uiState.destinations,
                handedness = uiState.settings.handedness,
                alignment = uiState.settings.boardAlignment,
                deals = uiState.deals,
                sideways = sideways && !phoneView,
            )
        }
    }
}

/** Phone view's board width: a typical modern phone's (412dp), chosen with the user. */
private val PHONE_WIDTH = 412.dp

/** From this short side (Material's medium window), the screen is a tablet's and phone view applies. */
private const val TABLET_WIDTH_DP = 600
private val TOOLBAR_SPACE = 88.dp
private const val STUCK_MESSAGE_MILLIS = 5_000L
