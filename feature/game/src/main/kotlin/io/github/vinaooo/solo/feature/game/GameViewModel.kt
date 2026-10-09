package io.github.vinaooo.solo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.autocomplete.AutoCompleter
import io.github.vinaooo.solo.domain.hint.DeadEndDetector
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.model.Achievement
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.model.badges
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.usecase.FinishGame
import io.github.vinaooo.solo.domain.usecase.LoseGame
import io.github.vinaooo.solo.domain.usecase.RecordAchievements
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.solo.domain.usecase.SaveGame
import io.github.vinaooo.solo.domain.usecase.StartNewGame
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.vinkit.shell.Ticker
import io.github.vinaooo.vinkit.shell.give
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList") // Each collaborator is one small, separately tested responsibility.
@HiltViewModel
class GameViewModel @Inject constructor(
    private val startNewGame: StartNewGame,
    private val resumeGame: ResumeGame,
    private val saveGame: SaveGame,
    private val finishGame: FinishGame,
    private val loseGame: LoseGame,
    private val settingsRepository: SettingsRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val engine: GameEngine,
    private val resolver: MoveResolver,
    private val hints: HintEngine,
    private val autoCompleter: AutoCompleter,
    private val feedback: GameFeedback,
    private val recordAchievements: RecordAchievements,
    achievements: AchievementRepository,
    deadEndDetector: DeadEndDetector,
    @SearchDispatcher searchDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val state = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = state.asStateFlow()

    // The clock stops once time is up; against the clock, it also stops while auto-complete plays a game already
    // won, so the time can't run out in the middle of it.
    private val clock = Ticker(viewModelScope, CLOCK_TICK_MILLIS) {
        state.value.session
            ?.takeIf { it.isInProgress && !it.state.isTimeUp }
            ?.takeUnless { it.state.secondsLeft != null && autoCompleteJob?.isActive == true }
            ?.let {
                val next = it.tick(it.state.elapsedSeconds + 1, engine)
                show(next, keepHint = true)
                if (next.state.isTimeUp) timeUp()
            }
    }
    private var autoCompleteJob: Job? = null

    // A result for a position the game has already left is stale and dropped.
    private val deadEnds = DeadEndWatcher(viewModelScope, deadEndDetector, searchDispatcher) { position, stuck ->
        state.update {
            when {
                it.session?.state?.hasSamePilesAs(position) != true -> it
                else -> it.copy(isStuck = stuck, showStuckTip = it.showStuckTip || (stuck && !it.isStuck))
            }
        }
    }

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            appSettingsRepository.settings.collect { settings -> state.update { it.copy(appSettings = settings) } }
        }
        viewModelScope.launch {
            // Badges unlocked from now on are shown; the ones already earned when the screen opened are not.
            var known: Set<Achievement>? = null
            achievements.progress.map { it.badges }.distinctUntilChanged().collect { now ->
                val new = known?.let { now - it }.orEmpty()
                known = now
                if (new.isNotEmpty()) state.update { it.copy(earned = it.earned + new) }
            }
        }
        viewModelScope.launch {
            val resumed = resumeGame()
            val settings = settingsRepository.settings.first()
            show(resumed ?: startNewGame(settings.drawMode, settings.gameMode, settings.difficulty))
            // A game dealt now, not one picked up where it was left, is dealt on the board.
            if (resumed == null) state.update { it.copy(deals = it.deals + 1) }
            // Its time ran out while the app was away.
            if (resumed?.state?.isTimeUp == true) timeUp()
        }
        viewModelScope.launch {
            // Switching the draw mode, game mode or difficulty in Settings (confirmed there) deals a new game with
            // them; all together, as one change, so a single settings write never deals twice.
            settingsRepository.settings.map { Triple(it.drawMode, it.gameMode, it.difficulty) }.distinctUntilChanged()
                .drop(1)
                .collect { (drawMode, mode, difficulty) -> newGame(restart = false, drawMode, mode, difficulty) }
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.Tap -> withSession { resolver.resolveTap(it.state, intent.pile, intent.cardIndex) }
            is GameIntent.Drop -> withSession {
                resolver.resolveDrop(it.state, intent.from, intent.cardIndex, intent.to)
            }
            GameIntent.Undo -> step(GameSession::undo, Announcement.Undone)
            GameIntent.Redo -> step(GameSession::redo, Announcement.Redone)
            GameIntent.Hint -> showHint()
            GameIntent.AutoComplete -> autoComplete()
            GameIntent.NewGame, GameIntent.RestartDeal -> newGame(restart = intent == GameIntent.RestartDeal)
            GameIntent.MessageShown,
            GameIntent.AutoCompleteTipShown,
            GameIntent.StuckTipShown,
            GameIntent.BadgesShown,
            -> state.update { it.shown(intent) }
            GameIntent.Resume -> clock.start()
            GameIntent.Pause -> pause()
            GameIntent.ReportBug -> Unit
        }
    }

    private inline fun withSession(resolve: (GameSession) -> Move?) {
        val session = state.value.session ?: return
        val move = resolve(session)
        val next = move?.let { session.play(it, engine) }
        if (next == null) {
            feedback.give(FeedbackEvent.REJECTED, state.value.appSettings)
        } else {
            onPlayed(next)
            state.announce(announcementFor(session.state, move, next.state))
        }
    }

    private fun onPlayed(next: GameSession) {
        show(next)
        if (next.state.isWon) {
            feedback.give(FeedbackEvent.WIN, state.value.appSettings)
            viewModelScope.launch {
                val record = finishGame(next)
                state.update { it.copy(winRecord = record) }
            }
        } else {
            feedback.give(FeedbackEvent.MOVE, state.value.appSettings)
            viewModelScope.launch {
                saveGame(next)
                recordAchievements.played()
            }
        }
    }

    /** Undo or redo: it plays out like a move (a redo can even win), then says what it did. */
    private fun step(move: (GameSession) -> GameSession?, announcement: Announcement) {
        if (state.value.isTimeUp) return
        val next = state.value.session?.let(move) ?: return
        onPlayed(next)
        state.announce(announcement)
    }

    private fun showHint() {
        val session = state.value.session?.takeUnless { it.state.isTimeUp } ?: return
        val hint = hints.bestHint(session.state)
        state.update { if (hint == null) it.copy(message = GameMessage.NO_MOVES) else it.copy(hint = hint) }
        hint?.let { state.announce(hintAnnouncement(session.state, it)) }
    }

    private fun autoComplete() {
        if (!state.value.canAutoComplete || state.value.isTimeUp || autoCompleteJob?.isActive == true) return
        state.update { it.copy(isAutoCompleting = true) }
        state.announce(Announcement.AutoCompleting)
        autoCompleteJob = viewModelScope.launch {
            // Each step starts from the latest session, so clock ticks between steps are kept.
            while (true) {
                val next = state.value.session
                    ?.takeUnless { it.state.isWon }
                    ?.let { session -> autoCompleter.nextMove(session.state)?.let { session.play(it, engine) } }
                    ?: break
                onPlayed(next)
                if (!next.state.isWon) delay(AUTO_COMPLETE_STEP_MILLIS)
            }
            state.update { it.copy(isAutoCompleting = false) }
        }
    }

    private fun newGame(
        restart: Boolean,
        drawMode: DrawMode = state.value.settings.drawMode,
        mode: GameMode = state.value.settings.gameMode,
        difficulty: Difficulty = state.value.settings.difficulty,
    ) {
        autoCompleteJob?.cancel()
        viewModelScope.launch {
            // A restarted deal keeps its own modes and difficulty, whatever Settings say now.
            val replay = state.value.session?.takeIf { restart }
            val session = if (replay != null) {
                startNewGame(replay.state.drawMode, replay.state.mode, replay.state.difficulty, replay.seed)
            } else {
                startNewGame(drawMode, mode, difficulty)
            }
            // A new game is dealt on the board; a restarted deal just goes back to its start.
            state.update {
                it.copy(winRecord = null, isAutoCompleting = false, deals = it.deals + if (restart) 0 else 1)
            }
            show(session)
        }
    }

    private fun pause() {
        clock.stop()
        // A game whose time ran out was already recorded as lost and removed; saving it again would count it twice.
        state.value.session?.takeUnless { it.state.isWon || it.state.isTimeUp }
            ?.let { viewModelScope.launch { saveGame(it) } }
    }

    /** Counter time ran out: the game is lost, and recorded so (once: [LoseGame] removes the saved game). */
    private fun timeUp() {
        clock.stop()
        state.announce(Announcement.TimeUp)
        state.value.session?.let { viewModelScope.launch { loseGame(it) } }
    }

    private fun show(session: GameSession, keepHint: Boolean = false) {
        val canAutoComplete = autoCompleter.canAutoComplete(session.state)
        val tip = canAutoComplete &&
            !state.value.canAutoComplete &&
            state.value.settings.autoCompleteTipsShown < AUTO_COMPLETE_TIPS
        if (tip) {
            viewModelScope.launch {
                settingsRepository.update { it.copy(autoCompleteTipsShown = it.autoCompleteTipsShown + 1) }
            }
        }
        // A clock tick leaves the cards where they are: no need to look for a dead end again.
        if (state.value.session?.state?.hasSamePilesAs(session.state) != true) deadEnds.check(session.state)
        state.update {
            it.copy(
                session = session,
                hint = if (keepHint) it.hint else null,
                canAutoComplete = canAutoComplete,
                showAutoCompleteTip = canAutoComplete && (it.showAutoCompleteTip || tip),
                // A clock tick leaves the cards where they are, so their destinations don't change.
                destinations = if (it.session?.state?.hasSamePilesAs(session.state) == true) {
                    it.destinations
                } else {
                    resolver.destinationsOf(session.state)
                },
            )
        }
    }

    private companion object {
        const val CLOCK_TICK_MILLIS = 1_000L
        const val AUTO_COMPLETE_STEP_MILLIS = 120L
        const val AUTO_COMPLETE_TIPS = 3
    }
}

/** Numbers each announcement, so saying the same thing twice in a row is still spoken twice. */
private fun MutableStateFlow<GameUiState>.announce(announcement: Announcement) {
    update { it.copy(announcement = Announced(announcement, (it.announcement?.sequence ?: 0) + 1)) }
}

/** Clears what the screen has just shown. */
private fun GameUiState.shown(intent: GameIntent) = when (intent) {
    GameIntent.MessageShown -> copy(message = null)
    GameIntent.AutoCompleteTipShown -> copy(showAutoCompleteTip = false)
    GameIntent.StuckTipShown -> copy(showStuckTip = false)
    else -> copy(earned = emptyList())
}
