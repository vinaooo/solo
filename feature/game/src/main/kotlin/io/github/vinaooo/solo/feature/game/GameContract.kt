package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.GameSession

data class GameUiState(
    val session: GameSession? = null,
    val settings: Settings = Settings(),
    val hint: Move? = null,
    val canAutoComplete: Boolean = false,
    val isAutoCompleting: Boolean = false,
    /** Point out the auto-complete button that just appeared, the first few times it does. */
    val showAutoCompleteTip: Boolean = false,
    val winRecord: ScoreRecord? = null,
    val message: GameMessage? = null,
    /** Where each face-up card can legally go; TalkBack offers these as actions. */
    val destinations: Map<CardSpot, List<PileRef>> = emptyMap(),
    val announcement: Announced? = null,
)

/** The card at [index] in [pile]. */
data class CardSpot(val pile: PileRef, val index: Int)

enum class GameMessage { NO_MOVES }

sealed interface GameIntent {
    data class Tap(val pile: PileRef, val cardIndex: Int) : GameIntent

    data class Drop(val from: PileRef, val cardIndex: Int, val to: PileRef) : GameIntent

    data object Undo : GameIntent

    data object Redo : GameIntent

    data object Hint : GameIntent

    data object AutoComplete : GameIntent

    data object NewGame : GameIntent

    data object RestartDeal : GameIntent

    data object MessageShown : GameIntent

    data object AutoCompleteTipShown : GameIntent

    /** The screen became visible: the clock may run. */
    data object Resume : GameIntent

    /** The screen went to the background: stop the clock and save. */
    data object Pause : GameIntent
}
