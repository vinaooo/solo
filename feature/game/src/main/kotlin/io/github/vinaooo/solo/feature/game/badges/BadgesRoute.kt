package io.github.vinaooo.solo.feature.game.badges

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.domain.model.Achievement
import io.github.vinaooo.solo.domain.model.Ladder
import io.github.vinaooo.solo.feature.game.R
import io.github.vinaooo.vinkit.achievements.Badge
import io.github.vinaooo.vinkit.achievements.BadgesScreen

@Composable
fun BadgesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BadgesViewModel = hiltViewModel()) {
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    BadgesScreen(Achievement.entries.map { badge(it) }, unlocked, onBack, modifier)
}

/** A badge in vinkit's terms: stored by its name, shown in the UI language. */
@Composable
internal fun badge(achievement: Achievement): Badge {
    val ladder = achievement.ladder
    return if (ladder != null) {
        val (name, note) = LADDER_TEXT.getValue(ladder)
        Badge(
            achievement.name,
            pluralStringResource(name, achievement.count, achievement.count),
            pluralStringResource(note, achievement.count, achievement.count),
        )
    } else {
        val (name, note) = BADGE_TEXT.getValue(achievement)
        Badge(achievement.name, stringResource(name), stringResource(note))
    }
}

private val LADDER_TEXT = mapOf(
    Ladder.PLAYED to (R.plurals.badge_played_name to R.plurals.badge_played_note),
    Ladder.DAYS to (R.plurals.badge_days_name to R.plurals.badge_days_note),
    Ladder.WON to (R.plurals.badge_won_name to R.plurals.badge_won_note),
    Ladder.STREAK to (R.plurals.badge_streak_name to R.plurals.badge_streak_note),
)

private val BADGE_TEXT = mapOf(
    Achievement.WIN_STANDARD to (R.string.badge_win_standard_name to R.string.badge_win_standard_note),
    Achievement.WIN_VEGAS to (R.string.badge_win_vegas_name to R.string.badge_win_vegas_note),
    Achievement.WIN_VEGAS_CUMULATIVE to
        (R.string.badge_win_vegas_cumulative_name to R.string.badge_win_vegas_cumulative_note),
    Achievement.WIN_COUNTER_TIME to (R.string.badge_win_counter_time_name to R.string.badge_win_counter_time_note),
    Achievement.WIN_EVERY_MODE to (R.string.badge_win_every_mode_name to R.string.badge_win_every_mode_note),
    Achievement.WIN_DRAW_3 to (R.string.badge_win_draw_3_name to R.string.badge_win_draw_3_note),
    Achievement.WIN_HARD to (R.string.badge_win_hard_name to R.string.badge_win_hard_note),
    Achievement.WIN_NO_UNDO to (R.string.badge_win_no_undo_name to R.string.badge_win_no_undo_note),
    Achievement.WIN_UNDER_3_MIN to (R.string.badge_win_under_3_min_name to R.string.badge_win_under_3_min_note),
    Achievement.WIN_UNDER_100_MOVES to
        (R.string.badge_win_under_100_moves_name to R.string.badge_win_under_100_moves_note),
    Achievement.VEGAS_PROFIT to (R.string.badge_vegas_profit_name to R.string.badge_vegas_profit_note),
    Achievement.BANK_POSITIVE to (R.string.badge_bank_positive_name to R.string.badge_bank_positive_note),
)
