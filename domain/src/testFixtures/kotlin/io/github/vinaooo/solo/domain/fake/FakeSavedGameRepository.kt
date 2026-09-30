package io.github.vinaooo.solo.domain.fake

import io.github.vinaooo.solo.domain.repository.SavedGameRepository
import io.github.vinaooo.solo.domain.session.GameSession

class FakeSavedGameRepository(var saved: GameSession? = null) : SavedGameRepository {
    var saveCount = 0
        private set

    override suspend fun load(): GameSession? = saved

    override suspend fun save(session: GameSession) {
        saved = session
        saveCount++
    }

    override suspend fun clear() {
        saved = null
    }
}
