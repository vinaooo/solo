package io.github.vinaooo.solo.feature.settings

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.ThemeMode
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.kotest.matchers.nulls.shouldBeNull
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
    private val savedGames = FakeSavedGameRepository()

    private fun viewModel() = SettingsViewModel(repository, ResumeGame(savedGames))

    private fun gameInProgress() {
        val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
        savedGames.saved = GameSession(seed = 1, state = deal.copy(moves = 3))
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `every change is saved`() = runTest(dispatcher) {
        val vm = viewModel()

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
        val vm = viewModel()
        val collected = mutableListOf<Settings>()
        val job = backgroundScope.launch { vm.settings.collect { collected += it } }
        runCurrent()

        collected.last() shouldBe Settings(showTimer = false)
        job.cancel()
    }

    @Test
    fun `switching the draw mode mid-game waits for confirmation`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        runCurrent()
        vm.pendingDrawMode.value shouldBe DrawMode.THREE
        repository.current.value.drawMode shouldBe DrawMode.ONE

        vm.confirmDrawMode()
        runCurrent()
        vm.pendingDrawMode.value.shouldBeNull()
        repository.current.value.drawMode shouldBe DrawMode.THREE
    }

    @Test
    fun `dismissing keeps the draw mode`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        runCurrent()
        vm.dismissDrawMode()
        runCurrent()

        vm.pendingDrawMode.value.shouldBeNull()
        repository.current.value.drawMode shouldBe DrawMode.ONE
    }
}
