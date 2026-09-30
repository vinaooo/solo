package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    val current = MutableStateFlow(initial)

    override val settings: Flow<Settings> = current

    override suspend fun update(transform: (Settings) -> Settings) {
        current.value = transform(current.value)
    }
}
