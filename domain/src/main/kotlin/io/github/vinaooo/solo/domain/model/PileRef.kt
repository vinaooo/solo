package io.github.vinaooo.solo.domain.model

/** Identifies a pile on the table, used to translate taps and drags into [Move]s. */
sealed interface PileRef {
    data object Stock : PileRef

    data object Waste : PileRef

    data class Foundation(val index: Int) : PileRef

    data class Tableau(val index: Int) : PileRef
}
