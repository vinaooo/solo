package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.solo.feature.game.R

/** Counter time ran out: the game is lost. It can't be dismissed, only left for a new game or another try. */
@Composable
internal fun TimeUpDialog(onNewGame: () -> Unit, onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Rounded.TimerOff, contentDescription = null) },
        title = { Text(stringResource(R.string.time_up_title)) },
        text = { Text(stringResource(R.string.time_up_body)) },
        confirmButton = { TextButton(onClick = onNewGame) { Text(stringResource(R.string.new_game)) } },
        dismissButton = { TextButton(onClick = onRestart) { Text(stringResource(R.string.restart_deal)) } },
    )
}
