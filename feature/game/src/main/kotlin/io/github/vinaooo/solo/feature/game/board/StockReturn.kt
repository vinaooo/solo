package io.github.vinaooo.solo.feature.game.board

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.vinaooo.solo.domain.model.PileRef

/**
 * A card going between the stock and the waste. Going back to the stock face down (an undone draw, a recycled
 * waste), it shows its face until it reaches the stock, and slides under the stock's cards until its spring
 * settles. Only from the waste: those faces were seen, while a new deal's cards must fly in face down. Leaving the
 * stock (a draw), it comes out from under the stock's cover and flies above the waste.
 */
@Stable
internal class StockReturn(pile: PileRef) {
    var lastPile by mutableStateOf(pile)
    private var keepFace by mutableStateOf(false)
    private var underStock by mutableStateOf(false)
    private var leaving by mutableStateOf(false)

    fun isReturning(pile: PileRef) = pile == PileRef.Stock && lastPile == PileRef.Waste

    fun showsFace(pile: PileRef) = isReturning(pile) || keepFace

    fun slidesUnder(pile: PileRef) = isReturning(pile) || (underStock && pile == PileRef.Stock)

    /** Just drawn: from the composition that moves it to the waste until its spring settles there. */
    private fun isLeaving(pile: PileRef) = pile == PileRef.Waste && (lastPile == PileRef.Stock || leaving)

    /** Back to the stock quickly, like a pile gathered up; anything else at [default]. */
    fun <T> motion(pile: PileRef, default: FiniteAnimationSpec<T>, fast: FiniteAnimationSpec<T>) =
        if (isReturning(pile)) fast else default

    /**
     * Where [placed] sits among the cards: one on its way to a new pile ([flying]) is above every other card, like a
     * dragged one; one going back to the stock slides under the stock's cards, still above the waste it leaves.
     */
    fun zIndex(placed: PlacedCard, flying: Boolean): Float = when {
        slidesUnder(placed.pile) -> placed.z - placed.index - UNDER_STOCK_OFFSET
        flying && isLeaving(placed.pile) -> LEAVING_STOCK_Z + placed.index / LEAVING_ORDER_STEPS
        flying -> LIFTED_Z + placed.z
        else -> placed.z
    }

    fun start() {
        keepFace = true
        underStock = true
        leaving = false
    }

    fun leave() {
        keepFace = false
        underStock = false
        leaving = true
    }

    fun landed() {
        keepFace = false
    }

    fun settled() {
        keepFace = false
        underStock = false
        leaving = false
    }
}

/** Latches a card's return to the stock after composition, once it is known to have come from the waste. */
@Composable
internal fun rememberStockReturn(pile: PileRef): StockReturn {
    val stockReturn = remember { StockReturn(pile) }
    val returning = stockReturn.isReturning(pile)
    val drawn = pile == PileRef.Waste && stockReturn.lastPile == PileRef.Stock
    SideEffect {
        // A move cut short never settles, so each one resets what the one before set: drawn again before its
        // return settled, a card must not slide under the waste it now joins, and so on.
        when {
            returning -> stockReturn.start()
            drawn -> stockReturn.leave()
            pile != PileRef.Stock && pile != PileRef.Waste -> stockReturn.settled()
        }
        stockReturn.lastPile = pile
    }
    return stockReturn
}

/** Below every card of the stock, which a returning card is about to join, and above the waste. */
private const val UNDER_STOCK_OFFSET = 0.5f

/** Under the stock's cover ([STOCK_COVER_Z]) and above the waste, a drawn card keeping its order among the drawn. */
private const val LEAVING_STOCK_Z = STOCK_COVER_Z - 1
private const val LEAVING_ORDER_STEPS = 100f
