package io.github.vinaooo.solo.feature.game

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.domain.hint.DeadEndDetector
import io.github.vinaooo.solo.domain.model.GameState
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Checks each new position for a dead end on [dispatcher], off the main thread, and reports whether it's [onResult]
 * stuck. A new position cancels the check of the one before.
 */
internal class DeadEndWatcher(
    private val scope: CoroutineScope,
    private val detector: DeadEndDetector,
    private val dispatcher: CoroutineDispatcher,
    private val onResult: (position: GameState, stuck: Boolean) -> Unit,
) {
    private var job: Job? = null

    fun check(position: GameState) {
        job?.cancel()
        job = scope.launch { onResult(position, withContext(dispatcher) { detector.isStuck(position) }) }
    }
}

/** The dispatcher for searching a game for dead ends: CPU-bound work kept off the main thread. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object SearchDispatcherModule {
    @Provides
    @SearchDispatcher
    fun searchDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
