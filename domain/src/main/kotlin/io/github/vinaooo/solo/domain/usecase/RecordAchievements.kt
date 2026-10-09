package io.github.vinaooo.solo.domain.usecase

import io.github.vinaooo.solo.domain.model.AchievementFacts
import io.github.vinaooo.solo.domain.model.Achievements
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.key
import io.github.vinaooo.solo.domain.repository.Clock
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import kotlinx.coroutines.flow.first

/**
 * Marks today as played and unlocks the badges earned so far. Ladders read the stats, so badges a player already
 * deserved unlock the first time this runs. The game screen learns of new badges by watching the repository.
 */
class RecordAchievements(
    private val achievements: AchievementRepository,
    private val stats: StatsRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** A counted game ended ([session] won, lost or abandoned in progress), after its stats were updated. */
    suspend fun gameEnded(session: GameSession) {
        val won = session.state.isWon
        settings.update { it.copy(winStreak = if (won) it.winStreak + 1 else 0) }
        record(session)
    }

    /** A move was played today. */
    suspend fun played() = record(ended = null)

    private suspend fun record(ended: GameSession?) {
        val all = GameMode.entries.associateWith { stats.observe(it.key).first() }
        val winStreak = settings.settings.first().winStreak
        val today = clock.today()
        achievements.update { progress ->
            val days = progress.collected[Achievements.DAYS_PLAYED].orEmpty() + today.toString()
            val facts = AchievementFacts(
                played = all.values.sumOf { it.played },
                won = all.values.sumOf { it.won },
                modesWon = all.filterValues { it.won > 0 }.keys,
                winStreak = winStreak,
                dayStreak = Achievements.dayStreak(days, today),
                ended = ended,
            )
            Achievements.after(
                progress.copy(collected = progress.collected + (Achievements.DAYS_PLAYED to days)),
                facts,
            )
        }
    }
}
