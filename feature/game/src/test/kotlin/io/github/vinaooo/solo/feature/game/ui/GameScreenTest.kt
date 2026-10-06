package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameMessage
import io.github.vinaooo.solo.feature.game.GameUiState
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val dealt = Dealer().deal(SeededShuffler(3), DrawMode.ONE)
    private val playing = GameUiState(
        session = GameSession(seed = 3, state = dealt.copy(score = 25, moves = 7, elapsedSeconds = 83)),
    )
    private val intents = mutableListOf<GameIntent>()
    private var openedScores = false
    private var openedSettings = false

    private fun show(state: GameUiState) {
        compose.setContent {
            SoloTheme {
                GameScreen(
                    uiState = state,
                    onIntent = { intents += it },
                    onOpenScores = { openedScores = true },
                    onOpenSettings = { openedSettings = true },
                )
            }
        }
    }

    private val Card.tag get() = "card_${suit}_$rank"

    @Test
    fun `shows score, moves and time`() {
        show(playing)

        compose.onNodeWithContentDescription("Score, 25").assertExists()
        compose.onNodeWithContentDescription("Moves, 7").assertExists()
        compose.onNodeWithContentDescription("Time, 1 minute 23 seconds").assertExists()
    }

    @Test
    fun `hides the time in Vegas`() {
        show(playing.copy(session = GameSession(seed = 3, state = dealt.copy(mode = GameMode.VEGAS, score = -47))))

        compose.onNodeWithContentDescription("Time", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Score, ", substring = true).assertExists()
    }

    @Test
    fun `opens scores and settings`() {
        show(playing)

        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()

        openedScores shouldBe true
        openedSettings shouldBe true
    }

    @Test
    fun `undo is disabled until there is a move to undo`() {
        show(playing)

        compose.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }

    @Test
    fun `toolbar buttons send their intents`() {
        val drawn = playing.session!!.play(Move.Draw, engine)!!
        show(playing.copy(session = drawn))

        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNodeWithContentDescription("Hint").performClick()
        compose.onNodeWithContentDescription("New game").performClick()
        compose.onNodeWithText("Restart this deal").performClick()
        compose.onNodeWithContentDescription("New game").performClick()
        compose.onNodeWithText("New game").performClick()

        intents shouldContainExactly
            listOf(GameIntent.Undo, GameIntent.Hint, GameIntent.RestartDeal, GameIntent.NewGame)
    }

    @Test
    fun `auto-complete shows only when the game can finish by itself`() {
        show(playing)
        compose.onNodeWithContentDescription("Auto-complete").assertDoesNotExist()
    }

    @Test
    fun `auto-complete button sends its intent`() {
        show(playing.copy(canAutoComplete = true))

        compose.onNodeWithContentDescription("Auto-complete").performClick()

        intents shouldContainExactly listOf(GameIntent.AutoComplete)
    }

    @Test
    fun `tapping a card asks to move it`() {
        show(playing)

        compose.onNodeWithTag(dealt.tableau[0].single().tag).performClick()

        intents shouldContainExactly listOf(GameIntent.Tap(PileRef.Tableau(0), 0))
    }

    @Test
    fun `dragging a card onto another column asks to drop it there`() {
        show(playing)
        val source = compose.onNodeWithTag(dealt.tableau[0].single().tag)
        val from = source.fetchSemanticsNode().boundsInRoot.center
        val to = compose.onNodeWithTag(dealt.tableau[3].last().tag).fetchSemanticsNode().boundsInRoot.center

        source.performTouchInput { drag(Offset(to.x - from.x, to.y - from.y)) }

        intents shouldContainExactly listOf(GameIntent.Drop(PileRef.Tableau(0), 0, PileRef.Tableau(3)))
    }

    @Test
    fun `a card dropped back where it started asks for nothing`() {
        show(playing)
        val source = compose.onNodeWithTag(dealt.tableau[0].single().tag)

        source.performTouchInput { drag(Offset(0f, height * 1.5f)) }

        intents shouldBe emptyList()
    }

    @Test
    fun `the no-moves message shows once and is acknowledged`() {
        show(playing.copy(message = GameMessage.NO_MOVES))

        compose.onNodeWithText("No useful moves left. Try a new game.").assertExists()
        compose.mainClock.advanceTimeBy(SNACKBAR_MILLIS)
        intents shouldContain GameIntent.MessageShown
    }

    @Test
    fun `the win dialog shows the result and starts a new game`() {
        show(playing.copy(winRecord = ScoreRecord(4321, 185, 97, DrawMode.ONE, 0)))

        compose.onNodeWithText("You won!").assertExists()
        compose.onNodeWithText("Score: 4321").assertExists()
        compose.onNodeWithText("Time: 3:05").assertExists()
        compose.onNodeWithText("Moves: 97").assertExists()
        compose.onNodeWithText("New game").performClick()

        intents shouldContainExactly listOf(GameIntent.NewGame)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `in landscape the stats sit left of the board and the toolbar right of it`() {
        show(playing)

        val board = compose.onNodeWithTag(dealt.tableau[0].single().tag).fetchSemanticsNode().boundsInRoot
        val score = compose.onNodeWithContentDescription("Score, 25").fetchSemanticsNode().boundsInRoot
        val scores = compose.onNodeWithContentDescription("Scores").fetchSemanticsNode().boundsInRoot
        val hint = compose.onNodeWithContentDescription("Hint").fetchSemanticsNode().boundsInRoot
        val lastColumn = compose.onNodeWithTag(dealt.tableau[6].last().tag).fetchSemanticsNode().boundsInRoot

        (score.right <= board.left) shouldBe true
        (scores.right <= board.left) shouldBe true
        (hint.left >= lastColumn.right) shouldBe true
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `in landscape every control still works`() {
        val drawn = playing.session!!.play(Move.Draw, engine)!!
        show(playing.copy(session = drawn))

        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNodeWithContentDescription("Hint").performClick()
        compose.onNodeWithContentDescription("Scores").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithTag(dealt.tableau[0].single().tag).performClick()

        intents shouldContainExactly listOf(GameIntent.Undo, GameIntent.Hint, GameIntent.Tap(PileRef.Tableau(0), 0))
        openedScores shouldBe true
        openedSettings shouldBe true
    }

    @Test
    fun `no board is drawn while the game loads`() {
        compose.mainClock.autoAdvance = false
        show(GameUiState())
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithTag(dealt.tableau[0].single().tag).assertDoesNotExist()
        compose.onNodeWithContentDescription("Score, 0").assertExists()
    }

    /** A finger drag in small steps, the way detectDragGestures sees a real one. */
    private fun TouchInjectionScope.drag(by: Offset) {
        down(center)
        repeat(DRAG_STEPS) { moveBy(by / DRAG_STEPS.toFloat()) }
        up()
    }

    private companion object {
        val engine = GameEngine()
        const val DRAG_STEPS = 20
        const val LANDSCAPE = "w891dp-h411dp-land"
        const val SNACKBAR_MILLIS = 5_000L
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `the new game menu speaks of a partida in Brazilian Portuguese`() {
        show(playing)

        compose.onNodeWithContentDescription("Nova partida").performClick()

        compose.onNodeWithText("Nova partida").assertExists()
        compose.onNodeWithText("Reiniciar partida").assertExists()
    }
}
