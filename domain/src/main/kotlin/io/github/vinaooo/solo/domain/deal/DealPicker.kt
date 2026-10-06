package io.github.vinaooo.solo.domain.deal

import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.repository.SeedSource
import io.github.vinaooo.solo.domain.repository.SettingsRepository

/**
 * Picks the seed of the next deal. Hard takes a random one. Easy and Normal walk through [WinnableDeals]' lists with
 * a cursor kept in the settings, which starts at a random place, so no deal comes back before a list's length of
 * deals has been played.
 */
class DealPicker(private val seedSource: SeedSource, private val settings: SettingsRepository) {

    suspend fun next(drawMode: DrawMode, mode: GameMode, difficulty: Difficulty): Long {
        if (difficulty == Difficulty.HARD) return seedSource.nextSeed()
        var cursor = 0L
        settings.update { current ->
            cursor = current.dealCursor ?: seedSource.nextSeed()
            current.copy(dealCursor = cursor + 1)
        }
        return WinnableDeals.seedAt(cursor, drawMode, mode, difficulty)
    }
}
