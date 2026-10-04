package io.github.vinaooo.solo.feature.game.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.feature.game.GameUiState
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.solo.feature.game.emailReport
import io.github.vinaooo.solo.feature.game.githubIssueUrl
import io.github.vinaooo.solo.feature.game.reportBody
import io.github.vinaooo.solo.feature.game.reportInfo

/**
 * "Report a bug": the player describes it, then sends it by email (with the board's [screenshot] and the game file
 * attached) or opens a prefilled GitHub issue (text only).
 */
@Composable
internal fun BugReportDialog(uiState: GameUiState, screenshot: ImageBitmap?, onDone: () -> Unit) {
    val context = LocalContext.current
    var description by rememberSaveable { mutableStateOf("") }
    val subject = stringResource(R.string.report_subject)
    fun body() = reportBody(context.reportInfo(uiState.settings, uiState.session), description)
    AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
        title = { Text(stringResource(R.string.report_bug)) },
        text = {
            Column {
                Text(stringResource(R.string.report_bug_body))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.report_bug_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                context.emailReport(subject, body(), screenshot?.asAndroidBitmap(), uiState.session)
                onDone()
            }) { Text(stringResource(R.string.report_by_email)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDone) { Text(stringResource(R.string.report_cancel)) }
                TextButton(onClick = {
                    val title =
                        description.lineSequence().firstOrNull()?.take(TITLE_LENGTH)?.ifBlank { null } ?: subject
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl(title, body()))))
                    onDone()
                }) { Text(stringResource(R.string.report_on_github)) }
            }
        },
    )
}

private const val TITLE_LENGTH = 70
