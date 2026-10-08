package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ThemeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppSettingsRepository(initial: AppSettings = AppSettings(themeColor = ThemeColor.GREEN)) :
    AppSettingsRepository {
    val current = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = current

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        current.value = transform(current.value)
    }
}
