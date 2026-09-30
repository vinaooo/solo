package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Card
import kotlin.random.Random

fun interface Shuffler {
    fun shuffle(cards: List<Card>): List<Card>
}

/** Deterministic Fisher–Yates shuffle: the same seed always deals the same game. */
class SeededShuffler(private val seed: Long) : Shuffler {
    override fun shuffle(cards: List<Card>): List<Card> = cards.shuffled(Random(seed))
}
