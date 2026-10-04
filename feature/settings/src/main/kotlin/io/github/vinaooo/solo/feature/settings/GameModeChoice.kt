package io.github.vinaooo.solo.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.ui.label
import io.github.vinaooo.solo.domain.model.GameMode

/**
 * The scoring mode, as a radio list: four modes with long names don't fit a segmented row on a phone, and each gets a
 * line saying what it is.
 */
@Composable
internal fun GameModeChoice(selected: GameMode, onSelect: (GameMode) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp).selectableGroup()) {
        Text(
            stringResource(R.string.game_mode),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
        )
        GameMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { onSelect(mode) })
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            ) {
                RadioButton(selected = mode == selected, onClick = null, modifier = Modifier.padding(12.dp))
                Column {
                    Text(stringResource(mode.label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(descriptions.getValue(mode)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val descriptions = mapOf(
    GameMode.STANDARD to R.string.mode_standard_note,
    GameMode.VEGAS to R.string.mode_vegas_note,
    GameMode.VEGAS_CUMULATIVE to R.string.mode_vegas_cumulative_note,
    GameMode.COUNTER_TIME to R.string.mode_counter_time_note,
)
