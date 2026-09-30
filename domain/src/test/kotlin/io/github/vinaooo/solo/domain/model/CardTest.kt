package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.solo.domain.card
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CardTest {

    @Test
    fun `hearts and diamonds are red, clubs and spades are black`() {
        Suit.HEARTS.color shouldBe SuitColor.RED
        Suit.DIAMONDS.color shouldBe SuitColor.RED
        Suit.CLUBS.color shouldBe SuitColor.BLACK
        Suit.SPADES.color shouldBe SuitColor.BLACK
    }

    @Test
    fun `ranks go from ace = 1 to king = 13`() {
        Rank.entries.map { it.value } shouldBe (1..13).toList()
    }

    @Test
    fun `cards of different colors are opposite`() {
        card("QH").hasOppositeColorOf(card("JS")).shouldBeTrue()
        card("QH").hasOppositeColorOf(card("JD")).shouldBeFalse()
    }

    @Test
    fun `flipping toggles only the face`() {
        val card = card("7C", faceUp = false)
        card.faceUp() shouldBe card.copy(isFaceUp = true)
        card.faceUp().faceDown() shouldBe card
    }
}
