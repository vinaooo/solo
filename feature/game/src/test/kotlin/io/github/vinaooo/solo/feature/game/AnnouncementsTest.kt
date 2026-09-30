package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class AnnouncementsTest {

    private val engine = GameEngine()
    private val nineOfHearts = Card(Suit.HEARTS, Rank.NINE, isFaceUp = true)
    private val tenOfSpades = Card(Suit.SPADES, Rank.TEN, isFaceUp = true)
    private val eightOfClubs = Card(Suit.CLUBS, Rank.EIGHT, isFaceUp = true)
    private val aceOfDiamonds = Card(Suit.DIAMONDS, Rank.ACE, isFaceUp = true)
    private val hiddenFive = Card(Suit.SPADES, Rank.FIVE)

    private val state = GameState(
        stock = listOf(Card(Suit.CLUBS, Rank.TWO)),
        waste = listOf(aceOfDiamonds),
        foundations = List(GameState.FOUNDATION_COUNT) { emptyList() },
        tableau = List(GameState.TABLEAU_COUNT) { column ->
            when (column) {
                0 -> listOf(hiddenFive, nineOfHearts, eightOfClubs)
                1 -> listOf(tenOfSpades)
                else -> emptyList()
            }
        },
        drawMode = DrawMode.ONE,
    )

    private fun played(move: Move): Announcement {
        val outcome = engine.apply(state, move).shouldBeInstanceOf<MoveOutcome.Applied>()
        return announcementFor(state, move, outcome.state)
    }

    @Test
    fun `a run names its bottom card, where it went and the card it turned over`() {
        played(Move.TableauToTableau(from = 0, to = 1, count = 2)) shouldBe
            Announcement.Moved(nineOfHearts, PileRef.Tableau(1), revealed = hiddenFive.faceUp())
    }

    @Test
    fun `a move that leaves a face-up card behind reveals nothing`() {
        played(Move.WasteToFoundation(2)) shouldBe Announcement.Moved(aceOfDiamonds, PileRef.Foundation(2))
    }

    @Test
    fun `drawing names the card turned over and recycling says so`() {
        played(Move.Draw) shouldBe Announcement.Drew(Card(Suit.CLUBS, Rank.TWO, isFaceUp = true))
        announcementFor(state, Move.Recycle, state) shouldBe Announcement.Recycled
    }

    @Test
    fun `a hint names the card it would move and where, or the stock action`() {
        hintAnnouncement(state, Move.TableauToTableau(0, 1, 2)) shouldBe
            Announcement.HintMove(nineOfHearts, PileRef.Tableau(1))
        hintAnnouncement(state, Move.WasteToFoundation(0)) shouldBe
            Announcement.HintMove(aceOfDiamonds, PileRef.Foundation(0))
        hintAnnouncement(state, Move.Draw) shouldBe Announcement.HintDraw
        hintAnnouncement(state, Move.Recycle) shouldBe Announcement.HintRecycle
    }

    @Test
    fun `cards on their way down from a foundation and single cards to a foundation are named`() {
        val onFoundation = state.copy(
            foundations = listOf(listOf(aceOfDiamonds)) + List(3) { emptyList() },
            tableau = state.tableau.mapIndexed { i, pile ->
                if (i ==
                    3
                ) {
                    listOf(Card(Suit.SPADES, Rank.TWO, true))
                } else {
                    pile
                }
            },
        )
        hintAnnouncement(onFoundation, Move.FoundationToTableau(0, 3)) shouldBe
            Announcement.HintMove(aceOfDiamonds, PileRef.Tableau(3))
        hintAnnouncement(state, Move.TableauToFoundation(0, 1)) shouldBe
            Announcement.HintMove(eightOfClubs, PileRef.Foundation(1))
        hintAnnouncement(state, Move.WasteToTableau(4)) shouldBe
            Announcement.HintMove(aceOfDiamonds, PileRef.Tableau(4))
    }
}
