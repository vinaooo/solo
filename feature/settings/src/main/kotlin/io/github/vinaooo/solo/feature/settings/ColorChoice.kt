package io.github.vinaooo.solo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.designsystem.theme.isDarkTheme
import io.github.vinaooo.solo.core.designsystem.theme.paletteScheme
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeColor

/**
 * A row of colored circles, one per [ThemeColor], each drawn in the primary color its scheme would give; the chosen
 * one carries a check. They share the row's width, so all eight fit on a phone.
 */
@Composable
internal fun ColorChoice(settings: Settings, onSelect: (ThemeColor) -> Unit) {
    val dark = isDarkTheme(settings.themeMode, isSystemInDarkTheme())
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.theme_color),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            ThemeColor.entries.forEach { color ->
                val scheme = paletteScheme(color, dark)
                val selected = color == settings.themeColor
                val name = stringResource(colorNames.getValue(color))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(SWATCH_TARGET)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(color) })
                        .semantics { contentDescription = name },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(SWATCH_SIZE).background(scheme.primary, CircleShape),
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary)
                    }
                }
            }
        }
    }
}

private val colorNames = mapOf(
    ThemeColor.GREEN to R.string.color_green,
    ThemeColor.TEAL to R.string.color_teal,
    ThemeColor.BLUE to R.string.color_blue,
    ThemeColor.INDIGO to R.string.color_indigo,
    ThemeColor.PURPLE to R.string.color_purple,
    ThemeColor.PINK to R.string.color_pink,
    ThemeColor.RED to R.string.color_red,
    ThemeColor.ORANGE to R.string.color_orange,
)
private val SWATCH_SIZE = 32.dp
private val SWATCH_TARGET = 48.dp
