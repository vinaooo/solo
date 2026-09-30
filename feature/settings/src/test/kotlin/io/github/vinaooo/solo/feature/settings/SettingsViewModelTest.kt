package io.github.vinaooo.solo.feature.settings

import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `every change is saved`() = runTest(dispatcher) {
        val vm = SettingsViewModel(repository)

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        vm.onChange(SettingsChange.ThemeModeChanged(ThemeMode.DARK))
        vm.onChange(SettingsChange.DynamicColorChanged(false))
        vm.onChange(SettingsChange.SoundChanged(false))
        vm.onChange(SettingsChange.HapticsChanged(false))
        vm.onChange(SettingsChange.ShowTimerChanged(false))
        runCurrent()

        repository.current.value shouldBe Settings(DrawMode.THREE, ThemeMode.DARK, false, false, false, false)
    }

    @Test
    fun `exposes the stored settings`() = runTest(dispatcher) {
        repository.current.value = Settings(showTimer = false)
        val vm = SettingsViewModel(repository)
        val collected = mutableListOf<Settings>()
        val job = backgroundScope.launch { vm.settings.collect { collected += it } }
        runCurrent()

        collected.last() shouldBe Settings(showTimer = false)
        job.cancel()
    }
}
