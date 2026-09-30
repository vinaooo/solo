package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.history.UndoHistory
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Plays random legal games and checks that the rules can never corrupt the board. */
class GameInvariantsPropertyTest {

    private val engine = GameEngine()
    private val rules = KlondikeRules()

    @Test
    fun `random legal play preserves all invariants`() = runTest {
        checkAll(ITERATIONS, Arb.long(), Arb.enum<DrawMode>()) { seed, drawMode ->
            val random = Random(seed)
            var state = Dealer().deal(SeededShuffler(seed), drawMode)
            repeat(MAX_MOVES) {
                val moves = rules.legalMoves(state)
                if (moves.isEmpty()) return@repeat
                state = (engine.apply(state, moves.random(random)) as MoveOutcome.Applied).state
                state.assertValid()
            }
        }
    }

    @Test
    fun `undo after any legal move restores the exact board`() = runTest {
        checkAll(ITERATIONS, Arb.long(), Arb.enum<DrawMode>()) { seed, drawMode ->
            val random = Random(seed)
            var state = Dealer().deal(SeededShuffler(seed), drawMode)
            repeat(MAX_MOVES) {
                val moves = rules.legalMoves(state)
                if (moves.isEmpty()) return@repeat
                val next = (engine.apply(state, moves.random(random)) as MoveOutcome.Applied).state
                val (restored, _) = UndoHistory().push(state, next).undo(next)!!
                restored.board() shouldBe state.board()
                state = next
            }
        }
    }

    @Test
    fun `legal moves are exactly the legal ones among every conceivable move`() = runTest {
        checkAll(ITERATIONS, Arb.long(), Arb.enum<DrawMode>()) { seed, drawMode ->
            val random = Random(seed)
            var state = Dealer().deal(SeededShuffler(seed), drawMode)
            repeat(random.nextInt(MAX_MOVES)) {
                val moves = rules.legalMoves(state)
                if (moves.isEmpty()) return@repeat
                state = (engine.apply(state, moves.random(random)) as MoveOutcome.Applied).state
            }
            rules.legalMoves(state).toSet() shouldBe everyConceivableMove().filter { rules.isLegal(state, it) }.toSet()
        }
    }

    private fun everyConceivableMove(): List<Move> {
        val indexes = -1..GameState.TABLEAU_COUNT
        return buildList {
            add(Move.Draw)
            add(Move.Recycle)
            for (a in indexes) {
                add(Move.WasteToTableau(a))
                add(Move.WasteToFoundation(a))
                for (b in indexes) {
                    add(Move.TableauToFoundation(a, b))
                    add(Move.FoundationToTableau(a, b))
                    for (count in 0..MAX_PILE) add(Move.TableauToTableau(a, b, count))
                }
            }
        }
    }

    private fun GameState.board() = listOf(stock, waste) + foundations + tableau

    private fun GameState.assertValid() {
        val cards = allCards()
        cards shouldHaveSize 52
        cards.map { it.suit to it.rank }.shouldNotContainDuplicates()
        stock.none { it.isFaceUp } shouldBe true
        waste.all { it.isFaceUp } shouldBe true
        foundations.forEach { pile ->
            pile.all { it.isFaceUp } shouldBe true
            pile.map { it.suit }.distinct().size shouldBe minOf(pile.size, 1)
            pile.map { it.rank.value } shouldBe (1..pile.size).toList()
        }
        tableau.forEach { pile ->
            if (pile.isNotEmpty()) pile.last().isFaceUp shouldBe true
            val faceUp = pile.dropWhile { !it.isFaceUp }
            faceUp.none { !it.isFaceUp } shouldBe true
        }
        score shouldBeGreaterThanOrEqual 0
    }

    private companion object {
        const val MAX_MOVES = 150
        const val ITERATIONS = 200
        const val MAX_PILE = 20
    }
}
