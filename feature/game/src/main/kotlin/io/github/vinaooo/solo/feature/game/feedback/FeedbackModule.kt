package io.github.vinaooo.solo.feature.game.feedback

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.feature.game.GameFeedback

@Module
@InstallIn(SingletonComponent::class)
internal interface FeedbackModule {
    @Binds
    fun gameFeedback(impl: AndroidGameFeedback): GameFeedback
}
