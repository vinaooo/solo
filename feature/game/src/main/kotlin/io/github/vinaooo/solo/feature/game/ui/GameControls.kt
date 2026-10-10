package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.ui.formatDollars
import io.github.vinaooo.solo.core.ui.spokenDollars
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.gameMode
import io.github.vinaooo.solo.domain.model.moves
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.formatElapsed
import io.github.vinaooo.vinkit.designsystem.spokenElapsed
import io.github.vinaooo.vinkit.shell.MenuOption
import io.github.vinaooo.vinkit.shell.R as ShellR
import io.github.vinaooo.vinkit.shell.ToolbarAction
import io.github.vinaooo.vinkit.shell.ToolbarTip

/** The stats: in a row on top in portrait, stacked in landscape's side column, which has the room for them. */
@Composable
internal fun GameInfo(uiState: GameUiState, large: Boolean) {
    if (large) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Stats(uiState, large = true) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Stats(uiState) }
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
        Stat(stringResource(ShellR.string.vinkit_time), formatElapsed(elapsed), large, spoken = spokenElapsed(elapsed))
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

/**
 * Undo, redo, hint and auto-complete, which joins the toolbar only when it can play; the first few times it does, a
 * bubble points it out.
 */
@Composable
internal fun toolbarActions(uiState: GameUiState, onIntent: (GameIntent) -> Unit): List<ToolbarAction> {
    val tip = stringResource(R.string.auto_complete_tip)
    return listOf(
        ToolbarAction.Button(
            Icons.AutoMirrored.Rounded.Undo,
            stringResource(ShellR.string.vinkit_undo),
            enabled = uiState.session?.canUndo == true,
        ) { onIntent(GameIntent.Undo) },
        ToolbarAction.Button(
            Icons.AutoMirrored.Rounded.Redo,
            stringResource(ShellR.string.vinkit_redo),
            enabled = uiState.session?.canRedo == true,
        ) { onIntent(GameIntent.Redo) },
        ToolbarAction.Button(Icons.Rounded.Lightbulb, stringResource(ShellR.string.vinkit_hint)) {
            onIntent(GameIntent.Hint)
        },
        ToolbarAction.Button(
            Icons.Rounded.AutoAwesome,
            stringResource(R.string.auto_complete),
            enabled = !uiState.isAutoCompleting,
            visible = uiState.canAutoComplete,
            tip = if (uiState.showAutoCompleteTip) {
                ToolbarTip(tip) {
                    onIntent(GameIntent.AutoCompleteTipShown)
                }
            } else {
                null
            },
        ) { onIntent(GameIntent.AutoComplete) },
    )
}

/** The new game menu: a new deal, or this deal again. */
@Composable
internal fun menuOptions(onIntent: (GameIntent) -> Unit): List<MenuOption> = listOf(
    MenuOption(Icons.Rounded.Style, stringResource(ShellR.string.vinkit_new_game)) { onIntent(GameIntent.NewGame) },
    MenuOption(Icons.Rounded.Refresh, stringResource(ShellR.string.vinkit_restart)) {
        onIntent(GameIntent.RestartDeal)
    },
)

/** What a win shows: Vegas's winnings, or the score (counter time ranks by time alone), then the time and moves. */
@Composable
internal fun winLines(record: ScoreRecord): List<String> = listOfNotNull(
    when {
        record.gameMode.isVegas -> stringResource(R.string.win_money, formatDollars(record.points))
        record.gameMode != GameMode.COUNTER_TIME -> stringResource(ShellR.string.vinkit_win_score, record.points)
        else -> null
    },
    stringResource(ShellR.string.vinkit_win_time, formatElapsed(record.elapsedSeconds)),
    stringResource(R.string.win_moves, record.moves),
)
