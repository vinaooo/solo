package io.github.vinaooo.solo.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.solo.core.ui.label
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.vinkit.settings.Choice
import io.github.vinaooo.vinkit.settings.IconChoice
import io.github.vinaooo.vinkit.settings.IconOption

/** Solo's section of vinkit's Settings screen: the draw mode, the difficulty and the scoring mode. */
@Composable
internal fun GameSection(settings: Settings, onChange: (SettingsChange) -> Unit) {
    Choice(
        title = stringResource(R.string.draw_mode),
        options = listOf(
            DrawMode.ONE to stringResource(R.string.draw_one),
            DrawMode.THREE to stringResource(R.string.draw_three),
        ),
        selected = settings.drawMode,
        onSelect = { onChange(SettingsChange.DrawModeChanged(it)) },
    )
    Choice(
        title = stringResource(R.string.difficulty),
        options = listOf(
            Difficulty.EASY to stringResource(R.string.difficulty_easy),
            Difficulty.NORMAL to stringResource(R.string.difficulty_normal),
            Difficulty.HARD to stringResource(R.string.difficulty_hard),
        ),
        selected = settings.difficulty,
        onSelect = { onChange(SettingsChange.DifficultyChanged(it)) },
    )
    // Four long names wouldn't fit a row on a phone; the icons do, and TalkBack reads each one's name.
    IconChoice(
        title = stringResource(R.string.game_mode),
        options = GameMode.entries.map {
            IconOption(it, icons.getValue(it), stringResource(it.label), stringResource(descriptions.getValue(it)))
        },
        selected = settings.gameMode,
        onSelect = { onChange(SettingsChange.GameModeChanged(it)) },
    )
}

private val icons = mapOf(
    GameMode.STANDARD to Icons.Rounded.Star,
    GameMode.VEGAS to Icons.Rounded.AttachMoney,
    GameMode.VEGAS_CUMULATIVE to Icons.Rounded.Savings,
    GameMode.COUNTER_TIME to Icons.Rounded.Timer,
)

private val descriptions = mapOf(
    GameMode.STANDARD to R.string.mode_standard_note,
    GameMode.VEGAS to R.string.mode_vegas_note,
    GameMode.VEGAS_CUMULATIVE to R.string.mode_vegas_cumulative_note,
    GameMode.COUNTER_TIME to R.string.mode_counter_time_note,
)
