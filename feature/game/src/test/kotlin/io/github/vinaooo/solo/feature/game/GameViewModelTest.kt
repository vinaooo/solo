package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.autocomplete.AutoCompleter
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.deal.SeededShuffler
import io.github.vinaooo.solo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.solo.domain.fake.FakeScoreRepository
import io.github.vinaooo.solo.domain.fake.FakeSettingsRepository
import io.github.vinaooo.solo.domain.fake.FakeStatsRepository
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.model.Card
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameState
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.PileRef
import io.github.vinaooo.solo.domain.model.Rank
import io.github.vinaooo.solo.domain.model.Settings
import io.github.vinaooo.solo.domain.model.Suit
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.usecase.FinishGame
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.solo.domain.usecase.SaveGame
import io.github.vinaooo.solo.domain.usecase.StartNewGame
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val savedGames = FakeSavedGameRepository()
    private val stats = FakeStatsRepository()
    private val scores = FakeScoreRepository()
    private val settings = FakeSettingsRepository()
    private val feedback = FakeGameFeedback()
    private val engine = GameEngine()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val created = mutableListOf<GameViewModel>()

    /**
     * Like runTest, but pauses every ViewModel at the end so its clock loop stops, as leaving the screen does.
     * It pauses even when the test fails; otherwise runTest waits for the clock forever and the failure hangs.
     */
    private fun gameTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            block()
        } finally {
            created.forEach { it.onIntent(GameIntent.Pause) }
        }
    }

    private fun TestScope.viewModel(): GameViewModel = GameViewModel(
        startNewGame = StartNewGame(savedGames, stats, Dealer(), seedSource = { 42 }),
        resumeGame = ResumeGame(savedGames),
        saveGame = SaveGame(savedGames),
        finishGame = FinishGame(scores, stats, savedGames, clock = { 5_000 }),
        settingsRepository = settings,
        engine = engine,
        resolver = MoveResolver(),
        hints = HintEngine(),
        autoCompleter = AutoCompleter(),
        feedback = feedback,
    ).also {
        created += it
        runCurrent()
    }

    private val GameViewModel.session get() = uiState.value.session!!

    private fun sessionWith(state: GameState) = GameSession(seed = 1, state = state)

    private fun fullRun(suit: Suit, upTo: Int = 13) = Rank.entries.take(upTo).map { Card(suit, it, isFaceUp = true) }

    /** Every card on the foundations except the kings, which sit face up on the tableau. */
    private val readyToAutoComplete = GameState(
        stock = emptyList(),
        waste = emptyList(),
        foundations = Suit.entries.map { fullRun(it, upTo = 12) },
        tableau = List(GameState.TABLEAU_COUNT) { i ->
            if (i < 4) listOf(Card(Suit.entries[i], Rank.KING, isFaceUp = true)) else emptyList()
        },
        drawMode = DrawMode.ONE,
        moves = 10,
    )

    @Test
    fun `resumes the saved game in progress`() = gameTest {
        val saved = sessionWith(Dealer().deal(SeededShuffler(9), DrawMode.ONE))
        savedGames.saved = saved

        viewModel().uiState.value.session shouldBe saved
    }

    @Test
    fun `deals a new game with the configured draw mode when nothing is saved`() = gameTest {
        settings.current.value = Settings(drawMode = DrawMode.THREE)

        val vm = viewModel()

        vm.session.seed shouldBe 42
        vm.session.state.drawMode shouldBe DrawMode.THREE
    }

    @Test
    fun `switching the draw mode deals a new game in that mode`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()

        settings.current.value = Settings(drawMode = DrawMode.THREE)
        runCurrent()

        vm.session.state.drawMode shouldBe DrawMode.THREE
        vm.session.state.moves shouldBe 0
        stats.stats.value.played shouldBe 1
    }

    @Test
    fun `other settings changes keep the game`() = gameTest {
        val vm = viewModel()
        val before = vm.session

        settings.current.value = Settings(showTimer = false)
        runCurrent()

        vm.session shouldBe before
    }

    @Test
    fun `tapping the stock draws, saves and gives feedback`() = gameTest {
        val vm = viewModel()

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()

        vm.session.state.waste.size shouldBe 1
        savedGames.saved shouldBe vm.session
        feedback.sounds shouldContainExactly listOf(FeedbackEvent.MOVE)
        feedback.haptics shouldContainExactly listOf(FeedbackEvent.MOVE)
    }

    @Test
    fun `an impossible tap is rejected with feedback and changes nothing`() = gameTest {
        val vm = viewModel()
        val before = vm.session

        vm.onIntent(GameIntent.Tap(PileRef.Waste, 0))
        runCurrent()

        vm.session shouldBe before
        feedback.haptics shouldContainExactly listOf(FeedbackEvent.REJECTED)
    }

    @Test
    fun `feedback respects the sound and haptics settings`() = gameTest {
        settings.current.value = Settings(soundEnabled = false, hapticsEnabled = false)
        val vm = viewModel()

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()

        feedback.sounds.shouldBeEmpty()
        feedback.haptics.shouldBeEmpty()
    }

    @Test
    fun `dropping a card applies the drop move, an illegal drop is rejected`() = gameTest {
        val state = readyToAutoComplete.copy(
            foundations = Suit.entries.map { emptyList() },
            tableau = List(GameState.TABLEAU_COUNT) { i ->
                when (i) {
                    0 -> listOf(Card(Suit.HEARTS, Rank.NINE, isFaceUp = true))
                    1 -> listOf(Card(Suit.SPADES, Rank.TEN, isFaceUp = true))
                    else -> emptyList()
                }
            },
        )
        savedGames.saved = sessionWith(state)
        val vm = viewModel()

        vm.onIntent(GameIntent.Drop(PileRef.Tableau(1), 0, PileRef.Tableau(0)))
        runCurrent()
        vm.session.state shouldBe state
        feedback.haptics shouldContainExactly listOf(FeedbackEvent.REJECTED)

        vm.onIntent(GameIntent.Drop(PileRef.Tableau(0), 0, PileRef.Tableau(1)))
        runCurrent()
        vm.session.state.tableau[1].size shouldBe 2
    }

    @Test
    fun `undo restores the previous board`() = gameTest {
        val vm = viewModel()
        val start = vm.session.state

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        vm.onIntent(GameIntent.Undo)
        runCurrent()

        vm.session.state.stock shouldBe start.stock
        vm.session.canUndo.shouldBeFalse()
    }

    @Test
    fun `hint highlights the best move and clears after the next move`() = gameTest {
        val vm = viewModel()

        vm.onIntent(GameIntent.Hint)
        runCurrent()
        vm.uiState.value.hint shouldBe HintEngine().bestHint(vm.session.state)

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()
        vm.uiState.value.hint.shouldBeNull()
    }

    @Test
    fun `asking for a hint with no useful move shows a message`() = gameTest {
        savedGames.saved = sessionWith(readyToAutoComplete.copy(foundations = Suit.entries.map { emptyList() }))
        val vm = viewModel()

        vm.onIntent(GameIntent.Hint)
        runCurrent()

        vm.uiState.value.message shouldBe GameMessage.NO_MOVES
        vm.onIntent(GameIntent.MessageShown)
        runCurrent()
        vm.uiState.value.message.shouldBeNull()
    }

    @Test
    fun `clock starts at the first move and charges the time penalty`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Resume)

        advanceTimeBy(5_001)
        vm.session.state.elapsedSeconds shouldBe 0

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        advanceTimeBy(10_001)

        vm.session.state.elapsedSeconds shouldBe 10
    }

    @Test
    fun `pausing stops the clock and saves`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Resume)
        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        advanceTimeBy(3_001)

        vm.onIntent(GameIntent.Pause)
        advanceTimeBy(20_000)

        vm.session.state.elapsedSeconds shouldBe 3
        savedGames.saved shouldBe vm.session
    }

    @Test
    fun `auto-complete is offered when possible and plays the game to a win`() = gameTest {
        savedGames.saved = sessionWith(readyToAutoComplete)
        val vm = viewModel()
        vm.uiState.value.canAutoComplete.shouldBeTrue()

        vm.onIntent(GameIntent.AutoComplete)
        advanceTimeBy(10_000)

        vm.session.state.isWon.shouldBeTrue()
        vm.uiState.value.isAutoCompleting.shouldBeFalse()
    }

    @Test
    fun `clock ticks during a long auto-complete are kept`() = gameTest {
        // Twenty cards to play at one every 120 ms: the clock ticks twice before the win.
        savedGames.saved = sessionWith(
            readyToAutoComplete.copy(
                foundations = Suit.entries.map { fullRun(it, upTo = 8) },
                tableau = List(GameState.TABLEAU_COUNT) { i ->
                    if (i <
                        4
                    ) {
                        Rank.entries.drop(8).reversed().map { Card(Suit.entries[i], it, isFaceUp = true) }
                    } else {
                        emptyList()
                    }
                },
            ),
        )
        val vm = viewModel()
        vm.onIntent(GameIntent.Resume)

        vm.onIntent(GameIntent.AutoComplete)
        advanceTimeBy(10_000)

        vm.session.state.isWon.shouldBeTrue()
        vm.session.state.elapsedSeconds shouldBe 2
        vm.uiState.value.winRecord.shouldNotBeNull().elapsedSeconds shouldBe 2
    }

    @Test
    fun `winning records the score and stats and shows the result`() = gameTest {
        savedGames.saved = sessionWith(readyToAutoComplete)
        val vm = viewModel()

        (0 until 4).forEach {
            vm.onIntent(GameIntent.Tap(PileRef.Tableau(it), 0))
            runCurrent()
        }

        val record = vm.uiState.value.winRecord.shouldNotBeNull()
        scores.records.value shouldContainExactly listOf(record)
        stats.stats.value shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
        savedGames.saved.shouldBeNull()
        feedback.sounds.last() shouldBe FeedbackEvent.WIN
    }

    @Test
    fun `new game abandons the current one`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()

        vm.onIntent(GameIntent.NewGame)
        runCurrent()

        vm.session.state.moves shouldBe 0
        stats.stats.value shouldBe GameStats(played = 1)
    }

    @Test
    fun `restart deals the same cards again`() = gameTest {
        val vm = viewModel()
        val original = vm.session
        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()

        vm.onIntent(GameIntent.RestartDeal)
        runCurrent()

        vm.session shouldBe original
    }

    @Test
    fun `dismissing the win result starts a new game`() = gameTest {
        savedGames.saved = sessionWith(readyToAutoComplete)
        val vm = viewModel()
        (0 until 4).forEach { vm.onIntent(GameIntent.Tap(PileRef.Tableau(it), 0)) }
        runCurrent()

        vm.onIntent(GameIntent.NewGame)
        runCurrent()

        vm.uiState.value.winRecord.shouldBeNull()
        vm.session.state.isWon.shouldBeFalse()
    }

    @Test
    fun `timer visibility follows the settings`() = gameTest {
        settings.current.value = Settings(showTimer = false)

        viewModel().uiState.value.settings.showTimer.shouldBeFalse()
    }

    private val nineOnTen = readyToAutoComplete.copy(
        foundations = Suit.entries.map { emptyList() },
        tableau = List(GameState.TABLEAU_COUNT) { i ->
            when (i) {
                0 -> listOf(Card(Suit.CLUBS, Rank.FIVE), Card(Suit.HEARTS, Rank.NINE, isFaceUp = true))
                1 -> listOf(Card(Suit.SPADES, Rank.TEN, isFaceUp = true))
                else -> emptyList()
            }
        },
    )

    @Test
    fun `every legal destination of each face-up card is offered`() = gameTest {
        savedGames.saved = sessionWith(nineOnTen)
        val vm = viewModel()

        vm.uiState.value.destinations shouldBe mapOf(CardSpot(PileRef.Tableau(0), 1) to listOf(PileRef.Tableau(1)))

        vm.onIntent(GameIntent.Tap(PileRef.Tableau(0), 1))
        runCurrent()
        // The nine now sits on the ten and the five it uncovered has nowhere to go.
        vm.uiState.value.destinations shouldBe emptyMap()
    }

    @Test
    fun `a move is announced with the card, its destination and the card it turned over`() = gameTest {
        savedGames.saved = sessionWith(nineOnTen)
        val vm = viewModel()

        vm.onIntent(GameIntent.Drop(PileRef.Tableau(0), 1, PileRef.Tableau(1)))
        runCurrent()

        vm.uiState.value.announcement?.announcement shouldBe Announcement.Moved(
            Card(Suit.HEARTS, Rank.NINE, isFaceUp = true),
            PileRef.Tableau(1),
            revealed = Card(Suit.CLUBS, Rank.FIVE, isFaceUp = true),
        )
    }

    @Test
    fun `drawing, undoing and hints are announced, each time with a new sequence number`() = gameTest {
        val vm = viewModel()

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()
        vm.uiState.value.announcement?.announcement shouldBe Announcement.Drew(vm.session.state.waste.last())

        vm.onIntent(GameIntent.Undo)
        val first = vm.uiState.value.announcement.shouldNotBeNull()
        first.announcement shouldBe Announcement.Undone

        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        vm.onIntent(GameIntent.Undo)
        val second = vm.uiState.value.announcement.shouldNotBeNull()
        second.announcement shouldBe Announcement.Undone
        (second.sequence > first.sequence).shouldBeTrue()

        vm.onIntent(GameIntent.Hint)
        vm.uiState.value.announcement?.announcement shouldBe
            hintAnnouncement(vm.session.state, vm.uiState.value.hint.shouldNotBeNull())
    }

    @Test
    fun `auto-complete is announced once instead of card by card`() = gameTest {
        savedGames.saved = sessionWith(readyToAutoComplete)
        val vm = viewModel()

        vm.onIntent(GameIntent.AutoComplete)
        advanceTimeBy(10_000)
        runCurrent()

        vm.session.state.isWon.shouldBeTrue()
        vm.uiState.value.announcement?.announcement shouldBe Announcement.AutoCompleting
    }

    @Test
    fun `an ordinary clock tick keeps the last announcement`() = gameTest {
        val vm = viewModel()
        vm.onIntent(GameIntent.Resume)
        vm.onIntent(GameIntent.Tap(PileRef.Stock, 0))
        runCurrent()
        val announced = vm.uiState.value.announcement

        advanceTimeBy(3_500)

        vm.uiState.value.announcement shouldBe announced
    }
}
