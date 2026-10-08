package io.github.vinaooo.solo.domain.model

/** Solo's own settings; the ones every vinkit game has (theme, feedback, hand, layout) are vinkit's `AppSettings`. */
data class Settings(
    val drawMode: DrawMode = DrawMode.ONE,
    val gameMode: GameMode = GameMode.STANDARD,
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** How many times the game has pointed out the auto-complete button; it stops after the first few. */
    val autoCompleteTipsShown: Int = 0,
    /** Where the next Easy or Normal deal is taken from its list (`DealPicker`); null until the first one. */
    val dealCursor: Long? = null,
)
