package io.github.vinaooo.solo.domain.hint

import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.emptyState
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.up
import io.github.vinaooo.solo.domain.withFoundation
import io.github.vinaooo.solo.domain.withTableau
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class DeadEndDetectorTest {

    private val detector = DeadEndDetector()

    private fun isStuck(state: GameState) = runBlocking { detector.isStuck(state) }

    private fun isStuck(with: DeadEndDetector, state: GameState) = runBlocking { with.isStuck(state) }

    /**
     * No empty column, every ace face down, and the face-up tops all red, so none of them can move. [stock] decides
     * whether there's a way out.
     */
    private fun blocked(vararg stock: String): GameState {
        val tops = listOf("KH", "KD", "QH", "QD", "JH", "JD", "10H")
        val hidden = listOf("AC", "AD", "AH", "AS", "2C", "2D", "2H")
        return tops.indices.fold(emptyState().copy(stock = down(*stock))) { state, i ->
            state.withTableau(i, down(hidden[i]) + up(tops[i]))
        }
    }

    @Test
    fun `stuck when no card in the stock fits anywhere`() {
        isStuck(blocked("3C", "5S", "7C")).shouldBeTrue()
    }

    @Test
    fun `a card that fits but leads nowhere is not a way out`() {
        // The nine of spades goes on the ten of hearts, and then nothing else can move.
        isStuck(blocked("3C", "9S", "7C")).shouldBeTrue()
    }

    @Test
    fun `not stuck when a stock card lets a column move and turn a card up`() {
        // The jack of spades goes on a red queen, then the ten of hearts on it, turning the two of hearts up.
        isStuck(blocked("3C", "JS", "7C")).shouldBeFalse()
    }

    @Test
    fun `not stuck when a card can go to a foundation`() {
        isStuck(blocked("3C", "5S").withFoundation(0, up("AS", "2S", "3S", "4S"))).shouldBeFalse()
    }

    @Test
    fun `an empty column frees a king and turns a card up`() {
        isStuck(blocked("3C").withTableau(6, emptyList())).shouldBeFalse()
    }

    @Test
    fun `a card taken back off a foundation can open the way`() {
        // The jack of spades comes down onto a red queen, then the ten of hearts on it, turning the two of hearts up.
        val spades = up("AS", "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS")
        isStuck(blocked("3C", "5D", "7C").withFoundation(0, spades)).shouldBeFalse()
    }

    @Test
    fun `a search cut short assumes there is still a way`() {
        isStuck(DeadEndDetector(maxPositions = 1), blocked("3C", "5S", "7C")).shouldBeFalse()
    }
}
