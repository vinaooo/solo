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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
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
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameMessage
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.GameViewModel
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.solo.feature.game.SoloReports
import io.github.vinaooo.solo.feature.game.badges.badge
import io.github.vinaooo.solo.feature.game.board.GameBoard
import io.github.vinaooo.solo.feature.game.gameReport
import io.github.vinaooo.vinkit.achievements.R as AchievementsR
import io.github.vinaooo.vinkit.shell.GameFrame
import io.github.vinaooo.vinkit.shell.GameSurface
import io.github.vinaooo.vinkit.shell.GameToolbar
import io.github.vinaooo.vinkit.shell.LocalFrameInfo
import io.github.vinaooo.vinkit.shell.NavigationAction
import io.github.vinaooo.vinkit.shell.WinDialog
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBadges: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(GameIntent.Resume)
        onPauseOrDispose { viewModel.onIntent(GameIntent.Pause) }
    }
    GameScreen(uiState, viewModel::onIntent, onOpenScores, onOpenSettings, modifier, onOpenBadges)
}

@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onOpenScores: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenBadges: (() -> Unit)? = null,
) {
    val snackbar = remember { SnackbarHostState() }
    BadgesEarned(uiState, snackbar) { onIntent(GameIntent.BadgesShown) }
    val noMoves = stringResource(R.string.no_moves)
    LaunchedEffect(uiState.message) {
        if (uiState.message == GameMessage.NO_MOVES) {
            snackbar.showSnackbar(noMoves)
            onIntent(GameIntent.MessageShown)
        }
    }
    val announced = uiState.announcement
    GameSurface(
        announcement = announced?.let { announcementText(it.announcement) },
        announcementSequence = announced?.sequence ?: 0,
        modifier = modifier,
        color = SoloThemeExtras.cardColors.table,
        reportTarget = SoloReports,
        gameReport = { gameReport(uiState.settings, uiState.appSettings, uiState.session) },
    ) { reportBug ->
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
            GameFrame(
                settings = uiState.appSettings,
                info = { GameInfo(uiState, large = it.landscape) },
                board = { BoardOrLoading(uiState, onIntent) },
                toolbar = { frame ->
                    GameToolbar(
                        actions = toolbarActions(uiState, onIntent),
                        menuOptions = menuOptions(onIntent),
                        onReportBug = reportBug,
                        vertical = frame.landscape,
                        mirrored = frame.mirrored,
                    )
                },
                onOpenScores = onOpenScores,
                onOpenSettings = onOpenSettings,
                // The board sizes its cards from all the room it gets, and places itself in it.
                boardAspectRatio = null,
                // Landscape's side holds only the stats: the sideways board gets the rest.
                sideWidth = null,
                navigation = badgesButton(onOpenBadges),
            )
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = SNACKBAR_SPACE))
            StuckMessage(uiState.showStuckTip, { onIntent(GameIntent.StuckTipShown) }, Modifier.align(Alignment.Center))
        }
    }
    uiState.winRecord?.let { WinDialog(winLines(it), onNewGame = { onIntent(GameIntent.NewGame) }) }
    if (uiState.isTimeUp) {
        TimeUpDialog(onNewGame = { onIntent(GameIntent.NewGame) }, onRestart = { onIntent(GameIntent.RestartDeal) })
    }
}

/** The Badges screen's button beside Scores and Settings, when there is one. */
@Composable
private fun badgesButton(onOpenBadges: (() -> Unit)?): List<NavigationAction> {
    val label = stringResource(AchievementsR.string.vinkit_badges)
    return listOfNotNull(onOpenBadges?.let { NavigationAction(Icons.Rounded.MilitaryTech, label, it) })
}

/** Badges just unlocked, in a snackbar once no dialog covers the game (it would time out behind one). */
@Composable
private fun BadgesEarned(uiState: GameUiState, snackbar: SnackbarHostState, onShown: () -> Unit) {
    val earned = uiState.earned
    val covered = uiState.winRecord != null || uiState.isTimeUp
    val text = when (earned.size) {
        0 -> null
        1 -> stringResource(R.string.badge_earned, badge(earned.single()).name)
        else -> pluralStringResource(R.plurals.badges_earned, earned.size, earned.size)
    }
    LaunchedEffect(text, covered) {
        // Cleared once shown: clearing first would change the key and cancel the snackbar.
        if (text != null && !covered) {
            snackbar.showSnackbar(text)
            onShown()
        }
    }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoardOrLoading(uiState: GameUiState, onIntent: (GameIntent) -> Unit) {
    val session = uiState.session
    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
    } else {
        GameBoard(
            state = session.state,
            hint = uiState.hint,
            onIntent = onIntent,
            modifier = Modifier.fillMaxSize(),
            destinations = uiState.destinations,
            handedness = uiState.appSettings.handedness,
            alignment = uiState.appSettings.boardAlignment,
            deals = uiState.deals,
            // In landscape the cards lie sideways (phone view keeps its column, and the traditional board).
            sideways = LocalFrameInfo.current.landscape,
        )
    }
}

/** Keeps the snackbar above the toolbar. */
private val SNACKBAR_SPACE = 88.dp
private const val STUCK_MESSAGE_MILLIS = 5_000L
