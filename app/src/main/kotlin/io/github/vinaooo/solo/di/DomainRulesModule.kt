package io.github.vinaooo.solo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.solo.domain.autocomplete.AutoCompleter
import io.github.vinaooo.solo.domain.deal.Dealer
import io.github.vinaooo.solo.domain.hint.DeadEndDetector
import io.github.vinaooo.solo.domain.hint.HintEngine
import io.github.vinaooo.solo.domain.interaction.MoveResolver
import io.github.vinaooo.solo.domain.rules.GameEngine

/** The domain layer is plain Kotlin with no DI annotations, so its game rules are assembled here. */
@Module
@InstallIn(SingletonComponent::class)
object DomainRulesModule {
    @Provides fun gameEngine() = GameEngine()

    @Provides fun dealer() = Dealer()

    @Provides fun moveResolver() = MoveResolver()

    @Provides fun hintEngine() = HintEngine()

    @Provides fun autoCompleter() = AutoCompleter()

    @Provides fun deadEndDetector() = DeadEndDetector()
}
