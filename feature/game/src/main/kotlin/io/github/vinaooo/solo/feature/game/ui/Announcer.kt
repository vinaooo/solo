package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.designsystem.component.cardName
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.feature.game.Announced
import io.github.vinaooo.solo.feature.game.Announcement
import io.github.vinaooo.solo.feature.game.R

/**
 * Speaks each [announced] action through TalkBack, from an invisible live region: a live region is read whenever
 * its text changes. (`View.announceForAccessibility` is deprecated.)
 */
@Composable
internal fun Announcer(announced: Announced?, modifier: Modifier = Modifier) {
    val text = announced?.let { announcementText(it.announcement) }.orEmpty()
    // Two identical announcements in a row (two undos) would leave the text unchanged and the second unspoken,
    // so every other one ends with a character that isn't read aloud.
    val spoken = if (announced != null && announced.sequence % 2 == 1) text + ZERO_WIDTH_SPACE else text
    Box(
        modifier = modifier
            .size(1.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
            },
    )
}

@Composable
internal fun announcementText(announcement: Announcement): String = when (announcement) {
    is Announcement.Moved -> if (announcement.revealed == null) {
        stringResource(R.string.a11y_moved, cardName(announcement.card), placeName(announcement.to))
    } else {
        stringResource(
            R.string.a11y_moved_revealed,
            cardName(announcement.card),
            placeName(announcement.to),
            cardName(announcement.revealed),
        )
    }
    is Announcement.Drew -> stringResource(R.string.a11y_drew, cardName(announcement.card))
    Announcement.Recycled -> stringResource(R.string.a11y_recycled)
    Announcement.Undone -> stringResource(R.string.a11y_undone)
    Announcement.Redone -> stringResource(R.string.a11y_redone)
    is Announcement.HintMove ->
        stringResource(R.string.a11y_hint_move, cardName(announcement.card), placeName(announcement.to))
    Announcement.HintDraw -> stringResource(R.string.a11y_hint_draw)
    Announcement.HintRecycle -> stringResource(R.string.a11y_hint_recycle)
    Announcement.AutoCompleting -> stringResource(R.string.a11y_auto_completing)
    Announcement.TimeUp -> stringResource(R.string.time_up_title)
}

@Composable
private fun placeName(pile: PileRef): String = if (pile is PileRef.Tableau) {
    stringResource(R.string.a11y_to_column, pile.index + 1)
} else {
    stringResource(R.string.a11y_to_foundation)
}

private const val ZERO_WIDTH_SPACE = "\u200B"
