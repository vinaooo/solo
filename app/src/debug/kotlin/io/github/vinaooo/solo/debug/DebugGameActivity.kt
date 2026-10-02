package io.github.vinaooo.solo.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.solo.MainActivity
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.session.GameSession
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only. Saves a test game and opens it, to try what happens near the end of a game without playing one.
 * Start it with `-S`, so a running game screen can't overwrite the save, and pick the game with the `game` extra:
 * `adb shell am start -S -n io.github.vinaooo.solo/.debug.DebugGameActivity --es game near_stuck`
 * - `near_win` (the default): one move away from auto-complete.
 * - `near_stuck`: one card (the five of spades, in the stock) can still go to its foundation; then the game is stuck.
 */
@AndroidEntryPoint
class DebugGameActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val game = if (intent.getStringExtra("game") == "near_stuck") nearStuck() else nearWin()
        runBlocking { savedGames.save(GameSession(seed = 0, state = game)) }
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
            ),
        )
        finish()
    }

    private fun up(suit: Suit, rank: Rank) = Card(suit, rank, isFaceUp = true)

    /**
     * Every suit is up to ten on the foundations (spades to nine). Only the king of spades is face down, under the
     * ten of spades: moving the ten, to its foundation or onto a red jack, turns the king up and allows auto-complete.
     */
    private fun nearWin() = GameState(
        stock = emptyList(),
        waste = emptyList(),
        foundations = Suit.entries.map { suit ->
            Rank.entries.take(if (suit == Suit.SPADES) 9 else 10).map { up(suit, it) }
        },
        tableau = listOf(
            listOf(up(Suit.HEARTS, Rank.KING), up(Suit.CLUBS, Rank.QUEEN), up(Suit.HEARTS, Rank.JACK)),
            listOf(up(Suit.CLUBS, Rank.KING), up(Suit.DIAMONDS, Rank.QUEEN), up(Suit.CLUBS, Rank.JACK)),
            listOf(up(Suit.DIAMONDS, Rank.KING), up(Suit.SPADES, Rank.QUEEN), up(Suit.DIAMONDS, Rank.JACK)),
            listOf(Card(Suit.SPADES, Rank.KING), up(Suit.SPADES, Rank.TEN)),
            listOf(up(Suit.HEARTS, Rank.QUEEN), up(Suit.SPADES, Rank.JACK)),
            emptyList(),
            emptyList(),
        ),
        drawMode = DrawMode.ONE,
        moves = 100,
    )

    /**
     * No empty column, and each column a face-down card under a red top that can't move. Spades are on their
     * foundation up to four, so the five of spades in the stock can still go there; the rest of the stock fits nowhere.
     */
    private fun nearStuck(): GameState {
        val tops = listOf(Rank.KING, Rank.KING, Rank.QUEEN, Rank.QUEEN, Rank.JACK, Rank.JACK, Rank.TEN)
        val hidden = listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.CLUBS)
        return GameState(
            stock = listOf(Card(Suit.CLUBS, Rank.THREE), Card(Suit.SPADES, Rank.FIVE)),
            waste = emptyList(),
            foundations = listOf(Rank.entries.take(4).map { up(Suit.SPADES, it) }) + List(3) { emptyList() },
            tableau = tops.indices.map { i ->
                val rank = if (i < 4) Rank.ACE else Rank.TWO
                listOf(Card(hidden[i], rank), up(if (i % 2 == 0) Suit.HEARTS else Suit.DIAMONDS, tops[i]))
            },
            drawMode = DrawMode.ONE,
            moves = 40,
        )
    }
}
