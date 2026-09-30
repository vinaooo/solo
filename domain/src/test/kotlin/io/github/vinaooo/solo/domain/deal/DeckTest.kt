package io.github.vinaooo.solo.domain.deal

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DeckTest {

    @Test
    fun `standard deck has 52 unique face-down cards`() {
        val deck = Deck.standard()
        deck shouldHaveSize 52
        deck.shouldNotContainDuplicates()
        deck.none { it.isFaceUp } shouldBe true
    }
}
