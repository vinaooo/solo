package io.github.vinaooo.solo.feature.game.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.github.vinaooo.solo.core.designsystem.theme.SoloTheme
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.feature.game.Announced
import io.github.vinaooo.solo.feature.game.Announcement
import io.github.vinaooo.solo.feature.game.CardSpot
import io.github.vinaooo.solo.feature.game.GameIntent
import io.github.vinaooo.solo.feature.game.GameUiState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class GameAccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    private val nineOfHearts = Card(Suit.HEARTS, Rank.NINE, isFaceUp = true)
    private val tenOfSpades = Card(Suit.SPADES, Rank.TEN, isFaceUp = true)
    private val state = GameState(
        stock = listOf(Card(Suit.DIAMONDS, Rank.KING), Card(Suit.DIAMONDS, Rank.QUEEN)),
        waste = emptyList(),
        foundations = listOf(listOf(Card(Suit.CLUBS, Rank.ACE, isFaceUp = true))) + List(3) { emptyList() },
        tableau = List(GameState.TABLEAU_COUNT) { column ->
            when (column) {
                0 -> listOf(Card(Suit.SPADES, Rank.FIVE), Card(Suit.CLUBS, Rank.TWO), nineOfHearts)
                1 -> listOf(tenOfSpades)
                else -> emptyList()
            }
        },
        drawMode = DrawMode.ONE,
    )
    private val playing = GameUiState(
        session = GameSession(seed = 1, state = state),
        destinations = mapOf(CardSpot(PileRef.Tableau(0), 2) to listOf(PileRef.Tableau(1))),
    )
    private val intents = mutableListOf<GameIntent>()

    private fun show(uiState: GameUiState) {
        compose.setContent {
            SoloTheme { GameScreen(uiState, onIntent = { intents += it }, onOpenScores = {}, onOpenSettings = {}) }
        }
    }

    private val readable = !SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)

    private fun readableNode(description: String) =
        compose.onNode(hasContentDescription(description) and readable, useUnmergedTree = true)

    private fun SemanticsNodeInteraction.stateDescription() =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun spoken(): String =
        compose.onNode(liveRegion).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    @Test
    fun `cards say where they are and face-down cards are counted instead of read one by one`() {
        show(playing)

        readableNode("Stock, 2 cards").assertExists()
        readableNode("Ace of Clubs, foundation").assertExists()
        readableNode("Nine of Hearts, column 1, 2 face-down cards").assertExists()
        readableNode("Ten of Spades, column 2").assertExists()
        readableNode("Empty column 3").assertExists()
        compose.onAllNodes(hasContentDescription("Face-down card") and readable, useUnmergedTree = true)
            .fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun `a covered slot is not read`() {
        show(playing)

        // The first foundation slot lies under the ace, and the stock slot under the stock.
        compose.onAllNodes(hasContentDescription("Empty foundation") and readable, useUnmergedTree = true)
            .fetchSemanticsNodes().size shouldBe 3
        compose.onAllNodes(hasContentDescription("Empty stock") and readable, useUnmergedTree = true)
            .fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun `a card offers a move to each of its destinations`() {
        show(playing)

        val node = readableNode("Nine of Hearts, column 1, 2 face-down cards").fetchSemanticsNode()
        val actions = node.config[SemanticsActions.CustomActions]
        actions.map { it.label } shouldContainExactly listOf("Move to column 2")
        compose.runOnIdle { actions.single().action() }

        intents shouldContainExactly listOf(GameIntent.Drop(PileRef.Tableau(0), 2, PileRef.Tableau(1)))
    }

    @Test
    fun `one foundation action is offered even when several foundations fit`() {
        show(
            playing.copy(
                destinations = mapOf(
                    CardSpot(PileRef.Tableau(1), 0) to listOf(PileRef.Foundation(1), PileRef.Foundation(2)),
                ),
            ),
        )

        val node = readableNode("Ten of Spades, column 2").fetchSemanticsNode()
        node.config[SemanticsActions.CustomActions].map { it.label } shouldContainExactly
            listOf("Move to the foundation")
    }

    @Test
    fun `tapping is labelled, and a hinted card says so`() {
        show(playing.copy(hint = Move.TableauToTableau(from = 0, to = 1, count = 1)))

        val nine = readableNode("Nine of Hearts, column 1, 2 face-down cards")
        nine.fetchSemanticsNode().config[SemanticsActions.OnClick].label shouldBe "move"
        nine.stateDescription() shouldBe "Hint"
        readableNode("Ten of Spades, column 2").stateDescription() shouldBe null
        readableNode("Stock, 2 cards").fetchSemanticsNode().config[SemanticsActions.OnClick].label shouldBe
            "draw a card"
    }

    @Test
    fun `the live region stays tiny instead of covering the game`() {
        show(playing.copy(announcement = Announced(Announcement.Undone, sequence = 2)))

        compose.onNode(liveRegion).assertWidthIsEqualTo(1.dp).assertHeightIsEqualTo(1.dp)
    }

    @Test
    fun `announcements are spoken from a live region`() {
        val announced = Announced(
            Announcement.Moved(nineOfHearts, PileRef.Tableau(1), revealed = Card(Suit.CLUBS, Rank.TWO, true)),
            sequence = 2,
        )
        show(playing.copy(announcement = announced))

        spoken() shouldBe "Nine of Hearts to column 2. Revealed Two of Clubs"
    }

    @Test
    fun `saying the same thing twice in a row still changes the live region`() {
        var uiState by mutableStateOf(
            playing.copy(announcement = Announced(Announcement.Undone, sequence = 1)),
        )
        compose.setContent {
            SoloTheme { GameScreen(uiState, onIntent = {}, onOpenScores = {}, onOpenSettings = {}) }
        }
        val first = spoken()

        compose.runOnIdle { uiState = uiState.copy(announcement = Announced(Announcement.Undone, sequence = 2)) }

        spoken() shouldNotBe first
        spoken().trim('​') shouldBe "Move undone"
        first.trim('​') shouldBe "Move undone"
    }

    @Test
    fun `every announcement has its own words`() {
        var uiState by mutableStateOf(playing)
        compose.setContent { SoloTheme { GameScreen(uiState, onIntent = {}, onOpenScores = {}, onOpenSettings = {}) } }

        val texts = listOf(
            Announcement.Moved(nineOfHearts, PileRef.Foundation(0)),
            Announcement.Drew(tenOfSpades),
            Announcement.Recycled,
            Announcement.Undone,
            Announcement.HintMove(tenOfSpades, PileRef.Foundation(2)),
            Announcement.HintDraw,
            Announcement.HintRecycle,
            Announcement.AutoCompleting,
        ).map { announcement ->
            compose.runOnIdle { uiState = playing.copy(announcement = Announced(announcement, sequence = 2)) }
            spoken()
        }

        texts shouldContainExactly listOf(
            "Nine of Hearts to the foundation",
            "Drew Ten of Spades",
            "Waste turned over",
            "Move undone",
            "Hint: Ten of Spades to the foundation",
            "Hint: draw a card",
            "Hint: turn the waste over",
            "Finishing the game",
        )
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun `announcements and card places are in Brazilian Portuguese`() {
        show(playing.copy(announcement = Announced(Announcement.Moved(nineOfHearts, PileRef.Tableau(1)), 2)))

        spoken() shouldBe "Nove de Copas para a coluna 2"
        readableNode("Nove de Copas, coluna 1, 2 cartas viradas para baixo").assertExists()
        readableNode("Monte, 2 cartas").assertExists()
    }
}
