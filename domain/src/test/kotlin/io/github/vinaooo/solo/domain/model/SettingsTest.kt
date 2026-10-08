package io.github.vinaooo.solo.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `a first game is Standard, Draw 1, Normal`() {
        Settings() shouldBe
            Settings(drawMode = DrawMode.ONE, gameMode = GameMode.STANDARD, difficulty = Difficulty.NORMAL)
    }
}
