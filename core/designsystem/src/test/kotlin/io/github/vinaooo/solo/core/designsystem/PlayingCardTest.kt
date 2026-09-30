package io.github.vinaooo.solo.core.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.github.vinaooo.solo.core.designsystem.component.PlayingCard
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PlayingCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `face-up card announces rank and suit`() {
        compose.setContent { SoloTheme { PlayingCard(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = true)) } }

        compose.onNodeWithContentDescription("Queen of Hearts").assertIsDisplayed()
    }

    @Test
    fun `face-down card hides its identity`() {
        compose.setContent { SoloTheme { PlayingCard(Card(Suit.HEARTS, Rank.QUEEN, isFaceUp = false)) } }

        compose.onNodeWithContentDescription("Face-down card").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `descriptions are translated to Brazilian Portuguese`() {
        compose.setContent { SoloTheme { PlayingCard(Card(Suit.SPADES, Rank.ACE, isFaceUp = true)) } }

        compose.onNodeWithContentDescription("Ás de Espadas").assertIsDisplayed()
    }
}
