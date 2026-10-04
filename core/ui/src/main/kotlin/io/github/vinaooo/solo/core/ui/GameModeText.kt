package io.github.vinaooo.solo.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import io.github.vinaooo.solo.domain.model.GameMode
import kotlin.math.abs

/** A Vegas amount, such as "$13" or "-$52": the game's play money, the same symbol in every language. */
fun formatDollars(amount: Int): String = if (amount < 0) "-$${abs(amount)}" else "$$amount"

/** A Vegas amount the way TalkBack should say it, such as "-52 dollars", since "$" alone is often skipped. */
@Composable
fun spokenDollars(amount: Int): String = pluralStringResource(R.plurals.dollars, abs(amount), amount)

/** The mode's name, as Settings and the Scores screen show it. */
@get:StringRes
val GameMode.label: Int
    get() = when (this) {
        GameMode.STANDARD -> R.string.mode_standard
        GameMode.VEGAS -> R.string.mode_vegas
        GameMode.VEGAS_CUMULATIVE -> R.string.mode_vegas_cumulative
        GameMode.COUNTER_TIME -> R.string.mode_counter_time
    }
