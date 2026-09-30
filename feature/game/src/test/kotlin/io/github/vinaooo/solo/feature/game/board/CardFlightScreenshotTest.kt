package io.github.vinaooo.solo.feature.game.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.core.designsystem.theme.SoloThemeExtras
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures a move mid-flight: the moving card must be drawn over the cards it crosses. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h600dp-xhdpi")
class CardFlightScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun a_card_flying_to_a_foundation_passes_over_the_columns() {
        val state = mutableStateOf(beforeMove)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SoloTheme(themeMode = ThemeMode.LIGHT, dynamicColor = false) {
                Box(Modifier.fillMaxSize().background(SoloThemeExtras.cardColors.table)) {
                    GameBoard(state = state.value, hint = null, onIntent = {}, modifier = Modifier.fillMaxSize())
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()

        compose.runOnIdle { state.value = afterMove }
        compose.waitForIdle() // recompose and start the flight without moving the clock
        compose.mainClock.advanceTimeBy(MID_FLIGHT_MILLIS)

        compose.onRoot().captureRoboImage("src/test/screenshots/card_flight_to_foundation.png")
    }

    private companion object {
        const val MID_FLIGHT_MILLIS = 100L

        /** The ace of spades alone in the first column; every other column has cards the ace must fly over. */
        val beforeMove: GameState = run {
            val others = listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS)
                .flatMap { suit -> Rank.entries.drop(1).map { Card(suit, it, isFaceUp = false) } }
                .iterator()
            GameState(
                stock = emptyList(),
                waste = emptyList(),
                foundations = List(GameState.FOUNDATION_COUNT) { emptyList() },
                tableau = List(GameState.TABLEAU_COUNT) { column ->
                    if (column == 0) {
                        listOf(Card(Suit.SPADES, Rank.ACE, isFaceUp = true))
                    } else {
                        List(column) { others.next() } + others.next().faceUp()
                    }
                },
                drawMode = DrawMode.ONE,
            )
        }

        /** The ace moved to the foundation farthest away, above the last column. */
        val afterMove: GameState =
            (GameEngine().apply(beforeMove, Move.TableauToFoundation(from = 0, to = 3)) as MoveOutcome.Applied).state
    }
}
