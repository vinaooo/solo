package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.solo.core.designsystem.component.cardName
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.feature.game.Announcement
import io.github.vinaooo.solo.feature.game.R

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
