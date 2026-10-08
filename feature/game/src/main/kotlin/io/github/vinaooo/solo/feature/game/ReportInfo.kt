package io.github.vinaooo.solo.feature.game

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.BoardCodec
import io.github.vinaooo.solo.domain.session.GameSession
import java.io.File
import java.net.URLEncoder
import kotlinx.serialization.json.Json

/** What a bug report says about the app and the device; nothing personal. */
data class ReportInfo(
    val appVersion: String,
    val android: String,
    val device: String,
    val screen: String,
    val settings: Settings,
    val session: GameSession?,
)

/** The report's text: the player's [description], then the facts that help reproduce the bug. */
internal fun reportBody(info: ReportInfo, description: String): String = buildString {
    appendLine(description.ifBlank { "(no description)" })
    appendLine()
    appendLine("---")
    appendLine("App: ${info.appVersion}")
    appendLine("Android: ${info.android}")
    appendLine("Device: ${info.device}")
    appendLine("Screen: ${info.screen}")
    with(info.settings) {
        appendLine(
            "Settings: $drawMode, $gameMode, $difficulty, $handedness hand, board $boardAlignment, theme $themeMode, " +
                "dynamic color $dynamicColor, phone view $phoneView",
        )
    }
    info.session?.let {
        appendLine(
            "Game: seed ${it.seed}, ${it.state.mode}, ${it.state.drawMode}, ${it.state.difficulty}, " +
                "${it.state.moves} moves, " +
                "score ${it.state.score}, ${it.state.elapsedSeconds}s",
        )
        // The exact board, for the debug build to replay (BoardCodec).
        appendLine()
        appendLine("State:")
        appendLine("```")
        appendLine(BoardCodec.encode(it.state))
        appendLine("```")
    }
}.trimEnd()

/**
 * A new GitHub issue with [title] and [body] filled in, for the player to review and submit. Only the text fits a
 * link: the screenshot and the game file go by email.
 */
internal fun githubIssueUrl(title: String, body: String): String =
    "$ISSUES_URL?title=${URLEncoder.encode(title, UTF_8)}&body=${URLEncoder.encode(body, UTF_8)}"

/** Facts about this app and device. */
internal fun Context.reportInfo(settings: Settings, session: GameSession?): ReportInfo {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)

    @Suppress("DEPRECATION") // longVersionCode needs API 28; minSdk is 26.
    val build = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.P
    ) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode
    }
    val config = resources.configuration
    return ReportInfo(
        appVersion = "${packageInfo.versionName} ($build)",
        android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        device = "${Build.MANUFACTURER} ${Build.MODEL}",
        screen = "${config.screenWidthDp}x${config.screenHeightDp}dp, ${config.densityDpi}dpi",
        settings = settings,
        session = session,
    )
}

/**
 * Sends the report to [REPORT_EMAIL] through the player's email app, the board's [screenshot] and the game file (which
 * the debug build can load to replay the position) attached.
 */
internal fun Context.emailReport(subject: String, body: String, screenshot: Bitmap?, session: GameSession?) {
    val uris = attachments(screenshot, session).map { FileProvider.getUriForFile(this, "$packageName.reports", it) }
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        // A concrete type: Gmail takes several attachments of any type except "*/*".
        type = "multipart/mixed"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // The read permission reaches the email app through the clip data.
    uris.toClipData()?.let { send.clipData = it }
    // Straight to the email apps (the ones that open mailto: links), the report addressed and attached: the only one
    // directly, or a choice among them. A mailto: selector can't do this: Android refuses it with several attachments.
    val direct = emailApps().map { Intent(send).setPackage(it) }
    when {
        direct.size == 1 -> startActivity(direct.single())
        direct.isNotEmpty() -> startActivity(
            Intent.createChooser(direct.first(), null)
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, direct.drop(1).toTypedArray()),
        )
        // No email app set up: any app that can send the files.
        else -> startActivity(Intent.createChooser(send, null))
    }
}

/** The packages of the apps that open mailto: links. */
private fun Context.emailApps(): List<String> =
    packageManager.queryIntentActivities(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")), 0)
        .map { it.activityInfo.packageName }
        .distinct()

/** The screenshot and the game file, written to a fresh folder the file provider shares. */
private fun Context.attachments(screenshot: Bitmap?, session: GameSession?): List<File> {
    val dir = File(cacheDir, REPORTS_DIR).apply {
        deleteRecursively()
        mkdirs()
    }
    val shot = screenshot?.let { image ->
        File(dir, "screenshot.png").also { file ->
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        }
    }
    val game = session?.let { File(dir, "game.json").also { file -> file.writeText(Json.encodeToString(it)) } }
    return listOfNotNull(shot, game)
}

private fun List<Uri>.toClipData(): ClipData? = firstOrNull()?.let { first ->
    ClipData.newRawUri(null, first).also { clip -> drop(1).forEach { clip.addItem(ClipData.Item(it)) } }
}

/** Where reports go: the contact in the privacy policy, chosen with the user. */
internal const val REPORT_EMAIL = "vrpedrinho+solo@gmail.com"
private const val ISSUES_URL = "https://github.com/vinaooo/solo/issues/new"
private const val REPORTS_DIR = "reports"
private const val UTF_8 = "UTF-8"

/** PNG is lossless; the quality is ignored but required. */
private const val PNG_QUALITY = 100
