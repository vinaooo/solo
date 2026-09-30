package io.github.vinaooo.solo.domain.deal

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SeededShufflerTest {

    @Test
    fun `same seed always produces the same order`() = runTest {
        checkAll(Arb.long()) { seed ->
            SeededShuffler(seed).shuffle(Deck.standard()) shouldBe SeededShuffler(seed).shuffle(Deck.standard())
        }
    }

    @Test
    fun `shuffling is a permutation of the input`() = runTest {
        checkAll(Arb.long()) { seed ->
            SeededShuffler(seed).shuffle(Deck.standard()) shouldContainExactlyInAnyOrder Deck.standard()
        }
    }

    @Test
    fun `different seeds produce different orders`() {
        SeededShuffler(1).shuffle(Deck.standard()) shouldNotBe SeededShuffler(2).shuffle(Deck.standard())
    }
}
