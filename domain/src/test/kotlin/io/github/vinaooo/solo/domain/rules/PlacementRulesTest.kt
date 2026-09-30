package io.github.vinaooo.solo.domain.rules

import io.github.vinaooo.solo.domain.card
import io.github.vinaooo.solo.domain.down
import io.github.vinaooo.solo.domain.suitRun
import io.github.vinaooo.solo.domain.up
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlacementRulesTest {

    @Nested
    inner class Tableau {
        @Test
        fun `only a king goes on an empty column`() {
            PlacementRules.canStackOnTableau(card("KH"), emptyList()).shouldBeTrue()
            PlacementRules.canStackOnTableau(card("QH"), emptyList()).shouldBeFalse()
        }

        @Test
        fun `card must be one rank lower and opposite color`() {
            PlacementRules.canStackOnTableau(card("6H"), up("7S")).shouldBeTrue()
            PlacementRules.canStackOnTableau(card("6C"), up("7S")).shouldBeFalse()
            PlacementRules.canStackOnTableau(card("5H"), up("7S")).shouldBeFalse()
            PlacementRules.canStackOnTableau(card("8H"), up("7S")).shouldBeFalse()
        }

        @Test
        fun `cannot stack on a face-down card`() {
            PlacementRules.canStackOnTableau(card("6H"), down("7S")).shouldBeFalse()
        }
    }

    @Nested
    inner class Foundation {
        @Test
        fun `only an ace starts a foundation`() {
            PlacementRules.canStackOnFoundation(card("AS"), emptyList()).shouldBeTrue()
            PlacementRules.canStackOnFoundation(card("2S"), emptyList()).shouldBeFalse()
        }

        @Test
        fun `card must be same suit and one rank higher`() {
            PlacementRules.canStackOnFoundation(card("3S"), suitRun('S', upTo = 2)).shouldBeTrue()
            PlacementRules.canStackOnFoundation(card("3C"), suitRun('S', upTo = 2)).shouldBeFalse()
            PlacementRules.canStackOnFoundation(card("4S"), suitRun('S', upTo = 2)).shouldBeFalse()
        }

        @Test
        fun `face-down cards never go to a foundation`() {
            PlacementRules.canStackOnFoundation(card("AS", faceUp = false), emptyList()).shouldBeFalse()
        }
    }

    @Nested
    inner class Runs {
        @Test
        fun `a face-up alternating descending sequence is a movable run`() {
            PlacementRules.isMovableRun(up("9H", "8S", "7D")).shouldBeTrue()
            PlacementRules.isMovableRun(up("9H")).shouldBeTrue()
        }

        @Test
        fun `broken sequences are not movable`() {
            PlacementRules.isMovableRun(up("9H", "8D")).shouldBeFalse()
            PlacementRules.isMovableRun(up("9H", "7S")).shouldBeFalse()
            PlacementRules.isMovableRun(down("9H") + up("8S")).shouldBeFalse()
            PlacementRules.isMovableRun(emptyList()).shouldBeFalse()
            PlacementRules.isMovableRun(up("9H", "8S") + down("7D")).shouldBeFalse()
            PlacementRules.isMovableRun(up("8S", "9H")).shouldBeFalse()
        }
    }
}
