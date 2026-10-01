package io.github.vinaooo.solo.core.designsystem.component

import androidx.compose.ui.unit.Dp

/** The part of a card that still shows past the card on top of it: a [strip] that wide along one [edge]. */
data class CardCover(val edge: Edge, val strip: Dp) {
    enum class Edge { TOP, LEFT, RIGHT }
}
