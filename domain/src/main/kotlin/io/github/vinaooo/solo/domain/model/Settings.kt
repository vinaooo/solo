package io.github.vinaooo.solo.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val drawMode: DrawMode = DrawMode.ONE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showTimer: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
)
