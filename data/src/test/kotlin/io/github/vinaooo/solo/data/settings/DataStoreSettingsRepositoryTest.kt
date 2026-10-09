package io.github.vinaooo.solo.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.settings.DataStoreAppSettingsRepository
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

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") }
    }

    @Test
    fun `first launch reads the defaults`() = scope.runTest {
        DataStoreSettingsRepository(dataStore).settings.first() shouldBe Settings()
    }

    @Test
    fun `every setting is persisted`() = scope.runTest {
        val repository = DataStoreSettingsRepository(dataStore)
        val changed = Settings(
            drawMode = DrawMode.THREE,
            gameMode = GameMode.VEGAS,
            difficulty = Difficulty.EASY,
            autoCompleteTipsShown = 2,
            dealCursor = 4_321,
            winStreak = 7,
        )

        repository.update { changed }

        repository.settings.first() shouldBe changed
    }

    @Test
    fun `Solo's settings and vinkit's share the DataStore without overwriting each other`() = scope.runTest {
        val game = DataStoreSettingsRepository(dataStore)
        val app = DataStoreAppSettingsRepository(dataStore, AppSettings(themeColor = ThemeColor.GREEN))

        app.update { it.copy(themeMode = ThemeMode.DARK, handedness = Handedness.LEFT) }
        game.update { it.copy(drawMode = DrawMode.THREE) }
        app.update { it.copy(soundEnabled = false) }

        game.settings.first() shouldBe Settings(drawMode = DrawMode.THREE)
        app.settings.first() shouldBe AppSettings(
            themeMode = ThemeMode.DARK,
            themeColor = ThemeColor.GREEN,
            soundEnabled = false,
            handedness = Handedness.LEFT,
        )
    }

    @Test
    fun `the Vegas bank starts empty and keeps the balance it is given`() = scope.runTest {
        val bank = DataStoreVegasBankRepository(dataStore)
        bank.bank.first() shouldBe 0

        bank.set(-104)

        bank.bank.first() shouldBe -104
        DataStoreSettingsRepository(dataStore).settings.first() shouldBe Settings()
    }
}
