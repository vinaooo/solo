package io.github.vinaooo.solo.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the board sits in the height it has: at the top, or as low as the tallest possible column lets it. */
enum class BoardAlignment { TOP, BOTTOM }

data class Settings(
    val drawMode: DrawMode = DrawMode.ONE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showTimer: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
    val boardAlignment: BoardAlignment = BoardAlignment.TOP,
    /** How many times the game has pointed out the auto-complete button; it stops after the first few. */
    val autoCompleteTipsShown: Int = 0,
)
