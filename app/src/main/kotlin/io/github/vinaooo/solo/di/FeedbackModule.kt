package io.github.vinaooo.solo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.vinkit.shell.AndroidGameFeedback
import io.github.vinaooo.vinkit.shell.GameFeedback
import javax.inject.Singleton

/** vinkit's sounds and vibrations, one for the app: it loads the sounds once. */
@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {
    @Provides
    @Singleton
    fun gameFeedback(@ApplicationContext context: Context): GameFeedback = AndroidGameFeedback(context)
}
