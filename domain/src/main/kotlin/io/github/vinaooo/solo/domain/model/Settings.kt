package io.github.vinaooo.solo.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The app's colors when dynamic color is off or not available; green is Solo's brand. */
enum class ThemeColor { GREEN, TEAL, BLUE, INDIGO, PURPLE, PINK, RED, ORANGE }

/** Where the board sits in the height it has: at the top, or as low as the tallest possible column lets it. */
enum class BoardAlignment { TOP, BOTTOM }

/** Where phone view's board sits across the room it has on a tablet. */
enum class PhoneViewSide { LEFT, CENTER, RIGHT }

data class Settings(
    val drawMode: DrawMode = DrawMode.ONE,
    val gameMode: GameMode = GameMode.STANDARD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val themeColor: ThemeColor = ThemeColor.GREEN,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showTimer: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
    val boardAlignment: BoardAlignment = BoardAlignment.TOP,
    /** On a tablet, cards at the size a phone shows them, instead of filling the screen. */
    val phoneView: Boolean = false,
    val phoneViewSide: PhoneViewSide = PhoneViewSide.CENTER,
    /** How many times the game has pointed out the auto-complete button; it stops after the first few. */
    val autoCompleteTipsShown: Int = 0,
)
