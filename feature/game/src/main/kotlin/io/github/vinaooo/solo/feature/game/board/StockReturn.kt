package io.github.vinaooo.solo.feature.game.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.vinaooo.solo.domain.model.PileRef

/**
 * A card going from the waste back to the stock face down (an undone draw, a recycled waste). It shows its face
 * until it reaches the stock, and slides under the stock's cards until its spring settles. Only from the waste:
 * those faces were seen, while a new deal's cards must fly in face down.
 */
@Stable
internal class StockReturn(pile: PileRef) {
    var lastPile by mutableStateOf(pile)
    private var keepFace by mutableStateOf(false)
    private var underStock by mutableStateOf(false)

    fun isReturning(pile: PileRef) = pile == PileRef.Stock && lastPile == PileRef.Waste

    fun showsFace(pile: PileRef) = isReturning(pile) || keepFace

    fun slidesUnder(pile: PileRef) = isReturning(pile) || underStock

    fun start() {
        keepFace = true
        underStock = true
    }

    fun landed() {
        keepFace = false
    }

    fun settled() {
        keepFace = false
        underStock = false
    }
}

/** Latches a card's return to the stock after composition, once it is known to have come from the waste. */
@Composable
internal fun rememberStockReturn(pile: PileRef): StockReturn {
    val stockReturn = remember { StockReturn(pile) }
    val returning = stockReturn.isReturning(pile)
    SideEffect {
        if (returning) stockReturn.start()
        stockReturn.lastPile = pile
    }
    return stockReturn
}
