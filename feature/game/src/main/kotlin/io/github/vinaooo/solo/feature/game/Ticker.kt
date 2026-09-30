package io.github.vinaooo.solo.feature.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Calls [onTick] every [periodMillis] between [start] and [stop]. Starting it again while running does nothing. */
internal class Ticker(
    private val scope: CoroutineScope,
    private val periodMillis: Long,
    private val onTick: () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(periodMillis)
                onTick()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
