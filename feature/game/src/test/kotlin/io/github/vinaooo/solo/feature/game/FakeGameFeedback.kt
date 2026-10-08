package io.github.vinaooo.solo.feature.game

import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
class FakeGameFeedback : GameFeedback {
    val sounds = mutableListOf<FeedbackEvent>()
    val haptics = mutableListOf<FeedbackEvent>()

    override fun sound(event: FeedbackEvent) {
        sounds += event
    }

    override fun haptic(event: FeedbackEvent) {
        haptics += event
    }
}
