package io.github.vinaooo.solo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.solo.domain.autocomplete.AutoCompleter
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.model.Move
import io.github.vinaooo.solo.domain.repository.SettingsRepository
import io.github.vinaooo.solo.domain.rules.GameEngine
import io.github.vinaooo.solo.domain.session.GameSession
import io.github.vinaooo.solo.domain.usecase.FinishGame
import io.github.vinaooo.solo.domain.usecase.ResumeGame
import io.github.vinaooo.solo.domain.usecase.SaveGame
import io.github.vinaooo.solo.domain.usecase.StartNewGame
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("LongParameterList") // Each collaborator is one small, separately tested responsibility.
@HiltViewModel
class GameViewModel @Inject constructor(
    private val startNewGame: StartNewGame,
    private val resumeGame: ResumeGame,
    private val saveGame: SaveGame,
    private val finishGame: FinishGame,
    private val settingsRepository: SettingsRepository,
    private val engine: GameEngine,
    private val resolver: MoveResolver,
    private val hints: HintEngine,
    private val autoCompleter: AutoCompleter,
    private val feedback: GameFeedback,
) : ViewModel() {

    private val state = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = state.asStateFlow()

    private val clock = Ticker(viewModelScope, CLOCK_TICK_MILLIS) {
        state.value.session?.takeIf { it.isInProgress }?.let {
            show(it.tick(it.state.elapsedSeconds + 1, engine), keepHint = true)
        }
    }
    private var autoCompleteJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            val session = resumeGame() ?: startNewGame(settingsRepository.settings.first().drawMode)
            show(session)
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.Tap -> withSession { resolver.resolveTap(it.state, intent.pile, intent.cardIndex) }
            is GameIntent.Drop -> withSession {
                resolver.resolveDrop(it.state, intent.from, intent.cardIndex, intent.to)
            }
            GameIntent.Undo -> undo()
            GameIntent.Hint -> showHint()
            GameIntent.AutoComplete -> autoComplete()
            GameIntent.NewGame -> newGame(restart = false)
            GameIntent.RestartDeal -> newGame(restart = true)
            GameIntent.MessageShown -> state.update { it.copy(message = null) }
            GameIntent.Resume -> clock.start()
            GameIntent.Pause -> pause()
        }
    }

    private inline fun withSession(resolve: (GameSession) -> Move?) {
        val session = state.value.session ?: return
        val move = resolve(session)
        val next = move?.let { session.play(it, engine) }
        if (next == null) {
            feedback.give(FeedbackEvent.REJECTED, state.value.settings)
        } else {
            onPlayed(next)
        }
    }

    private fun onPlayed(next: GameSession) {
        show(next)
        if (next.state.isWon) {
            feedback.give(FeedbackEvent.WIN, state.value.settings)
            viewModelScope.launch {
                val record = finishGame(next)
                state.update { it.copy(winRecord = record) }
            }
        } else {
            feedback.give(FeedbackEvent.MOVE, state.value.settings)
            persist(next)
        }
    }

    private fun undo() {
        val undone = state.value.session?.undo() ?: return
        show(undone)
        feedback.give(FeedbackEvent.MOVE, state.value.settings)
        persist(undone)
    }

    private fun showHint() {
        val session = state.value.session ?: return
        val hint = hints.bestHint(session.state)
        state.update { if (hint == null) it.copy(message = GameMessage.NO_MOVES) else it.copy(hint = hint) }
    }

    private fun autoComplete() {
        if (!state.value.canAutoComplete || autoCompleteJob?.isActive == true) return
        state.update { it.copy(isAutoCompleting = true) }
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

    private fun newGame(restart: Boolean) {
        autoCompleteJob?.cancel()
        viewModelScope.launch {
            val replay = state.value.session?.takeIf { restart }
            val session = if (replay != null) {
                startNewGame(replay.state.drawMode, replay.seed)
            } else {
                startNewGame(state.value.settings.drawMode)
            }
            state.update { it.copy(winRecord = null, isAutoCompleting = false) }
            show(session)
        }
    }

    private fun pause() {
        clock.stop()
        state.value.session?.takeUnless { it.state.isWon }?.let(::persist)
    }

    private fun show(session: GameSession, keepHint: Boolean = false) {
        state.update {
            it.copy(
                session = session,
                hint = if (keepHint) it.hint else null,
                canAutoComplete = autoCompleter.canAutoComplete(session.state),
            )
        }
    }

    private fun persist(session: GameSession) {
        viewModelScope.launch { saveGame(session) }
    }

    private companion object {
        const val CLOCK_TICK_MILLIS = 1_000L
        const val AUTO_COMPLETE_STEP_MILLIS = 120L
    }
}
