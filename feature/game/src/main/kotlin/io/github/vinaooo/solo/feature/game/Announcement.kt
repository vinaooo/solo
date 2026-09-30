package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef

/** What TalkBack says after an action. The screen turns it into text in the device language. */
sealed interface Announcement {
    /** [card] (with any cards on top of it) went to [to]; [revealed] is the card the move turned face up. */
    data class Moved(val card: Card, val to: PileRef, val revealed: Card? = null) : Announcement

    data class Drew(val card: Card) : Announcement

    data object Recycled : Announcement

    data object Undone : Announcement

    data class HintMove(val card: Card, val to: PileRef) : Announcement

    data object HintDraw : Announcement

    data object HintRecycle : Announcement

    /** Auto-complete speaks once, instead of once for every card it plays. */
    data object AutoCompleting : Announcement
}

/** An [announcement] numbered by [sequence], so saying the same thing twice in a row still counts as new. */
data class Announced(val announcement: Announcement, val sequence: Int)

/** The announcement for [move], played from [before] to [after]. */
internal fun announcementFor(before: GameState, move: Move, after: GameState): Announcement = when (move) {
    Move.Draw -> Announcement.Drew(after.waste.last())
    Move.Recycle -> Announcement.Recycled
    else -> Announcement.Moved(movedCard(before, move), destination(move), revealed(before, move, after))
}

internal fun hintAnnouncement(state: GameState, move: Move): Announcement = when (move) {
    Move.Draw -> Announcement.HintDraw
    Move.Recycle -> Announcement.HintRecycle
    else -> Announcement.HintMove(movedCard(state, move), destination(move))
}

/** The card [move] picks up; for a run, the bottom one. */
private fun movedCard(state: GameState, move: Move): Card = when (move) {
    is Move.WasteToTableau, is Move.WasteToFoundation -> state.waste.last()
    is Move.TableauToTableau -> state.tableau[move.from].let { it[it.size - move.count] }
    is Move.TableauToFoundation -> state.tableau[move.from].last()
    is Move.FoundationToTableau -> state.foundations[move.from].last()
    Move.Draw, Move.Recycle -> error("$move does not pick up a card")
}

private fun destination(move: Move): PileRef = when (move) {
    is Move.WasteToTableau -> PileRef.Tableau(move.to)
    is Move.WasteToFoundation -> PileRef.Foundation(move.to)
    is Move.TableauToTableau -> PileRef.Tableau(move.to)
    is Move.TableauToFoundation -> PileRef.Foundation(move.to)
    is Move.FoundationToTableau -> PileRef.Tableau(move.to)
    Move.Draw, Move.Recycle -> error("$move does not go to a pile")
}

/** The card a move out of a column turned face up, if it uncovered one. */
private fun revealed(before: GameState, move: Move, after: GameState): Card? {
    val column = when (move) {
        is Move.TableauToTableau -> move.from
        is Move.TableauToFoundation -> move.from
        else -> return null
    }
    val left = after.tableau[column]
    val top = left.lastOrNull() ?: return null
    return top.takeIf { it.isFaceUp && !before.tableau[column][left.lastIndex].isFaceUp }
}
