package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.vinkit.core.ThemeMode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsTest {

    @Test
    fun `defaults follow the system with dynamic color and every feedback on`() {
        Settings() shouldBe Settings(
            drawMode = DrawMode.ONE,
            themeMode = ThemeMode.SYSTEM,
            dynamicColor = true,
            soundEnabled = true,
            hapticsEnabled = true,
        )
        with(Settings()) {
            dynamicColor shouldBe true
            soundEnabled shouldBe true
            hapticsEnabled shouldBe true
            difficulty shouldBe Difficulty.NORMAL
        }
    }
}
