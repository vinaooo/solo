package io.github.vinaooo.solo.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.component.EmptyPileSlot
import io.github.vinaooo.solo.core.designsystem.component.PlayingCard
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.model.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, themeMode: ThemeMode, dynamicColor: Boolean) {
        compose.setContent {
            SoloTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Card(Suit.SPADES, Rank.ACE, true),
                            Card(Suit.HEARTS, Rank.KING, true),
                            Card(Suit.DIAMONDS, Rank.TEN, true),
                            Card(Suit.CLUBS, Rank.SEVEN, true),
                        ).forEach { PlayingCard(it, Modifier.width(64.dp)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayingCard(Card(Suit.CLUBS, Rank.TWO, false), Modifier.width(64.dp))
                        PlayingCard(Card(Suit.HEARTS, Rank.QUEEN, true), Modifier.width(64.dp), highlighted = true)
                        EmptyPileSlot(Modifier.width(64.dp), icon = Icons.Rounded.Refresh)
                        EmptyPileSlot(Modifier.width(64.dp))
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun cards_light_brand() = capture("cards_light_brand", ThemeMode.LIGHT, dynamicColor = false)

    @Test
    fun cards_dark_brand() = capture("cards_dark_brand", ThemeMode.DARK, dynamicColor = false)

    @Test
    fun cards_light_dynamic() = capture("cards_light_dynamic", ThemeMode.LIGHT, dynamicColor = true)

    @Test
    fun cards_dark_dynamic() = capture("cards_dark_dynamic", ThemeMode.DARK, dynamicColor = true)
}
