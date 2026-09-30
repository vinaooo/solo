package io.github.vinaooo.solo.domain.model

/** Every player action in Klondike. Indexes refer to [GameState.tableau] and [GameState.foundations]. */
sealed interface Move {
    data object Draw : Move

    data object Recycle : Move

    data class WasteToTableau(val to: Int) : Move

    data class WasteToFoundation(val to: Int) : Move

    data class TableauToTableau(val from: Int, val to: Int, val count: Int) : Move

    data class TableauToFoundation(val from: Int, val to: Int) : Move

    data class FoundationToTableau(val from: Int, val to: Int) : Move
}
