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
 * Debug builds only. Saves a game one move away from auto-complete and opens it, to test what happens around
 * auto-complete without playing a whole game. Start it with `-S`, so a running game screen can't overwrite the save:
 * `adb shell am start -S -n io.github.vinaooo.solo/.debug.NearAutoCompleteActivity`
 */
@AndroidEntryPoint
class NearAutoCompleteActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking { savedGames.save(GameSession(seed = 0, state = nearAutoComplete())) }
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
            ),
        )
        finish()
    }

    /**
     * Every suit is up to ten on the foundations (spades to nine). Only the king of spades is face down, under the
     * ten of spades: moving the ten, to its foundation or onto a red jack, turns the king up and allows auto-complete.
     */
    private fun nearAutoComplete(): GameState {
        fun up(suit: Suit, rank: Rank) = Card(suit, rank, isFaceUp = true)
        return GameState(
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
    }
}
