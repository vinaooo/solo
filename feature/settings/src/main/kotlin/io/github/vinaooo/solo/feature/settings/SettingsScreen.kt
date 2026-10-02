package io.github.vinaooo.solo.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pendingDrawMode by viewModel.pendingDrawMode.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    SettingsScreen(settings, viewModel::onChange, onBack, modifier, privacyOptionsRequired, onOpenPrivacyOptions) {
        uriHandler.openUri(privacyPolicyUrl)
    }
    if (pendingDrawMode != null) DrawModeDialog(viewModel::confirmDrawMode, viewModel::dismissDrawMode)
}

@Composable
private fun DrawModeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.draw_mode_confirm_title)) },
        text = { Text(stringResource(R.string.draw_mode_confirm_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.draw_mode_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.draw_mode_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (SettingsChange) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { SettingsTopBar(onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.section_game))
            Choice(
                title = stringResource(R.string.draw_mode),
                options = listOf(DrawMode.ONE to R.string.draw_one, DrawMode.THREE to R.string.draw_three),
                selected = settings.drawMode,
                onSelect = { onChange(SettingsChange.DrawModeChanged(it)) },
            )
            Choice(
                title = stringResource(R.string.handedness),
                options = listOf(Handedness.LEFT to R.string.hand_left, Handedness.RIGHT to R.string.hand_right),
                selected = settings.handedness,
                onSelect = { onChange(SettingsChange.HandednessChanged(it)) },
            )
            ToggleRow(stringResource(R.string.show_timer), settings.showTimer) {
                onChange(SettingsChange.ShowTimerChanged(it))
            }

            SectionTitle(stringResource(R.string.section_appearance))
            Choice(
                title = stringResource(R.string.theme),
                options = listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ),
                selected = settings.themeMode,
                onSelect = { onChange(SettingsChange.ThemeModeChanged(it)) },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleRow(
                    title = stringResource(R.string.dynamic_color),
                    supporting = stringResource(R.string.dynamic_color_note),
                    checked = settings.dynamicColor,
                ) { onChange(SettingsChange.DynamicColorChanged(it)) }
            }

            SectionTitle(stringResource(R.string.section_feedback))
            ToggleRow(stringResource(R.string.sound), settings.soundEnabled) {
                onChange(SettingsChange.SoundChanged(it))
            }
            ToggleRow(stringResource(R.string.haptics), settings.hapticsEnabled) {
                onChange(SettingsChange.HapticsChanged(it))
            }
            PrivacySection(privacyOptionsRequired, onOpenPrivacyOptions, onOpenPrivacyPolicy)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.settings_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        },
    )
}

/**
 * The privacy policy link, which Google Play requires inside the app, and the consent form, which is offered only
 * where the law requires a way to change ad consent (GDPR, some US states).
 */
@Composable
private fun PrivacySection(
    privacyOptionsRequired: Boolean,
    onOpenPrivacyOptions: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    SectionTitle(stringResource(R.string.section_privacy))
    LinkRow(stringResource(R.string.privacy_policy), onClick = onOpenPrivacyPolicy)
    if (privacyOptionsRequired) {
        LinkRow(
            stringResource(R.string.privacy_options),
            supporting = stringResource(R.string.privacy_options_note),
            onClick = onOpenPrivacyOptions,
        )
    }
}

@Composable
private fun LinkRow(title: String, supporting: String? = null, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        supportingContent = supporting?.let { { Text(it) } },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Text(title)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun <T> Choice(title: String, options: List<Pair<T, Int>>, selected: T, onSelect: (T) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(stringResource(label)) }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, supporting: String? = null, onToggle: (Boolean) -> Unit) {
    ListItem(
        checked = checked,
        onCheckedChange = onToggle,
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        // The row draws its own container: line it up with the rest of the screen and keep neighbours apart.
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Text(title)
    }
}
