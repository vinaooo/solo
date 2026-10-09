package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAchievementRepository(initial: AchievementProgress = AchievementProgress()) : AchievementRepository {
    val current = MutableStateFlow(initial)

    override val progress: Flow<AchievementProgress> = current

    override suspend fun update(transform: (AchievementProgress) -> AchievementProgress) {
        current.value = transform(current.value)
    }
}
