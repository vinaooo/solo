package io.github.vinaooo.solo.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.vinaooo.solo.domain.model.BoardAlignment
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Handedness
import io.github.vinaooo.solo.domain.model.PhoneViewSide
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeColor
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var dir: File

    private val scope = TestScope(StandardTestDispatcher())

    private fun repository() = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") },
    )

    @Test
    fun `first launch reads the defaults`() = scope.runTest {
        repository().settings.first() shouldBe Settings()
    }

    @Test
    fun `every setting is persisted`() = scope.runTest {
        val repository = repository()
        val changed = Settings(
            drawMode = DrawMode.THREE,
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            themeColor = ThemeColor.PURPLE,
            soundEnabled = false,
            hapticsEnabled = false,
            showTimer = false,
            handedness = Handedness.LEFT,
            boardAlignment = BoardAlignment.BOTTOM,
            phoneView = true,
            phoneViewSide = PhoneViewSide.LEFT,
        )

        repository.update { changed }

        repository.settings.first() shouldBe changed
    }

    @Test
    fun `updates transform the current value`() = scope.runTest {
        val repository = repository()
        repository.update { it.copy(themeMode = ThemeMode.LIGHT) }
        repository.update { it.copy(showTimer = false) }

        repository.settings.first() shouldBe Settings(themeMode = ThemeMode.LIGHT, showTimer = false)
    }
}
