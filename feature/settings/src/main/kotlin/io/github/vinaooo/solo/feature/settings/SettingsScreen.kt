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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode

@Composable
fun SettingsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(settings, viewModel::onChange, onBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (SettingsChange) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.section_game))
            Choice(
                title = stringResource(R.string.draw_mode),
                options = listOf(DrawMode.ONE to R.string.draw_one, DrawMode.THREE to R.string.draw_three),
                selected = settings.drawMode,
                onSelect = { onChange(SettingsChange.DrawModeChanged(it)) },
            )
            Text(
                stringResource(R.string.draw_mode_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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
        }
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
