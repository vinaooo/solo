package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.solo.domain.model.Settings

enum class FeedbackEvent { MOVE, REJECTED, WIN }

/** Sound and vibration. The ViewModel decides when; implementations decide how. */
interface GameFeedback {
    fun sound(event: FeedbackEvent)

    fun haptic(event: FeedbackEvent)
}

/** Plays [event] through the channels the player left on in [settings]. */
internal fun GameFeedback.give(event: FeedbackEvent, settings: Settings) {
    if (settings.soundEnabled) sound(event)
    if (settings.hapticsEnabled) haptic(event)
}
