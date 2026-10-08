package io.github.vinaooo.solo.feature.settings

import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.fake.FakeAppSettingsRepository
import io.github.vinaooo.solo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.ThemeMode
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
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
    private val appRepository = FakeAppSettingsRepository()
    private val savedGames = FakeSavedGameRepository()

    private fun viewModel() = SettingsViewModel(repository, appRepository, ResumeGame(savedGames))

    private fun gameInProgress() {
        val deal = Dealer().deal(SeededShuffler(1), DrawMode.ONE)
        savedGames.saved = GameSession(seed = 1, state = deal.copy(moves = 3))
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `game changes go to Solo's settings, the others to vinkit's`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        vm.onAppChange { it.copy(themeMode = ThemeMode.DARK, soundEnabled = false) }
        runCurrent()

        repository.current.value shouldBe Settings(drawMode = DrawMode.THREE)
        appRepository.current.value.themeMode shouldBe ThemeMode.DARK
        appRepository.current.value.soundEnabled shouldBe false
    }

    @Test
    fun `exposes the stored settings`() = runTest(dispatcher) {
        repository.current.value = Settings(difficulty = Difficulty.EASY)
        appRepository.current.value = AppSettings(soundEnabled = false)
        val vm = viewModel()
        val collected = mutableListOf<Pair<Settings, AppSettings>>()
        val job = backgroundScope.launch { vm.settings.combine(vm.appSettings, ::Pair).collect { collected += it } }
        runCurrent()

        collected.last() shouldBe (Settings(difficulty = Difficulty.EASY) to AppSettings(soundEnabled = false))
        job.cancel()
    }

    @Test
    fun `switching the draw mode mid-game waits for confirmation`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        runCurrent()
        vm.pendingChange.value shouldBe SettingsChange.DrawModeChanged(DrawMode.THREE)
        repository.current.value.drawMode shouldBe DrawMode.ONE

        vm.confirmChange()
        runCurrent()
        vm.pendingChange.value.shouldBeNull()
        repository.current.value.drawMode shouldBe DrawMode.THREE
    }

    @Test
    fun `switching the game mode mid-game waits for confirmation too`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.GameModeChanged(GameMode.VEGAS))
        runCurrent()
        vm.pendingChange.value shouldBe SettingsChange.GameModeChanged(GameMode.VEGAS)
        repository.current.value.gameMode shouldBe GameMode.STANDARD

        vm.confirmChange()
        runCurrent()
        repository.current.value.gameMode shouldBe GameMode.VEGAS
    }

    @Test
    fun `switching the difficulty mid-game waits for confirmation too`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.DifficultyChanged(Difficulty.HARD))
        runCurrent()
        vm.pendingChange.value shouldBe SettingsChange.DifficultyChanged(Difficulty.HARD)
        repository.current.value.difficulty shouldBe Difficulty.NORMAL

        vm.confirmChange()
        runCurrent()
        repository.current.value.difficulty shouldBe Difficulty.HARD
    }

    @Test
    fun `without a game in progress, the game mode changes at once`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onChange(SettingsChange.GameModeChanged(GameMode.COUNTER_TIME))
        runCurrent()

        vm.pendingChange.value.shouldBeNull()
        repository.current.value.gameMode shouldBe GameMode.COUNTER_TIME
    }

    @Test
    fun `dismissing keeps the draw mode`() = runTest(dispatcher) {
        gameInProgress()
        val vm = viewModel()

        vm.onChange(SettingsChange.DrawModeChanged(DrawMode.THREE))
        runCurrent()
        vm.dismissChange()
        runCurrent()

        vm.pendingChange.value.shouldBeNull()
        repository.current.value.drawMode shouldBe DrawMode.ONE
    }
}
