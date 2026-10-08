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
import io.github.vinaooo.solo.domain.session.BoardCodec
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.session.decodeSession
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Debug builds only. Saves a test game and opens it, to try what happens near the end of a game without playing one.
 * Start it with `-S`, so a running game screen can't overwrite the save, and pick the game with the `game` extra:
 * `adb shell am start -S -n io.github.vinaooo.solo/.debug.DebugGameActivity --es game near_stuck`
 * - `near_win` (the default): one move away from auto-complete.
 * - `near_stuck`: one card (the five of spades, in the stock) can still go to its foundation; then the game is stuck.
 * - `tallest`: the last column as tall as a column gets, six face-down cards and a run from king to ace.
 *
 * Or replay a bug report:
 * - `--es state <code>`: the code in the report's "State:" block (GitHub or email), the exact board.
 * - `--es load game.json`: the report's attached game, with its moves to undo, pushed first to the app's own folder:
 *   `adb push game.json /sdcard/Android/data/io.github.vinaooo.solo/files/`
 */
@AndroidEntryPoint
class DebugGameActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra("state")?.let { GameSession(seed = 0, state = BoardCodec.decode(it)) }
            ?: intent.getStringExtra("load")?.let {
                decodeSession(File(getExternalFilesDir(null), it).readText())
            }
        val game = when (intent.getStringExtra("game")) {
            "near_stuck" -> nearStuck()
            "tallest" -> tallest()
            else -> nearWin()
        }
        runBlocking { savedGames.save(report ?: GameSession(seed = 0, state = game)) }
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

    /**
     * The last column holds six face-down cards and a run from the king of spades down to an ace, alternating
     * colours; the other columns are dealt as usual from what's left, and the rest is the stock.
     */
    private fun tallest(): GameState {
        val suits = listOf(Suit.SPADES, Suit.HEARTS, Suit.CLUBS, Suit.DIAMONDS)
        val run = Rank.entries.reversed().mapIndexed { i, rank -> up(suits[i % suits.size], rank) }
        val rest = Suit.entries.flatMap { suit -> Rank.entries.map { Card(suit, it) } }
            .filterNot { card -> run.any { it.suit == card.suit && it.rank == card.rank } }
            .iterator()
        val columns = (0 until 6).map { i -> List(i) { rest.next() } + rest.next().copy(isFaceUp = true) }
        val last = List(6) { rest.next() } + run
        return GameState(
            stock = rest.asSequence().toList(),
            waste = emptyList(),
            foundations = List(4) { emptyList() },
            tableau = columns + listOf(last),
            drawMode = DrawMode.ONE,
            moves = 60,
        )
    }
}
