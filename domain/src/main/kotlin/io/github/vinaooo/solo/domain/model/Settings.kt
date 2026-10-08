package io.github.vinaooo.solo.domain.model

import io.github.vinaooo.vinkit.core.BoardAlignment
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode

data class Settings(
    val drawMode: DrawMode = DrawMode.ONE,
    val gameMode: GameMode = GameMode.STANDARD,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val themeColor: ThemeColor = ThemeColor.GREEN,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val handedness: Handedness = Handedness.RIGHT,
    val boardAlignment: BoardAlignment = BoardAlignment.TOP,
    /** On a tablet, cards at the size a phone shows them, instead of filling the screen. */
    val phoneView: Boolean = false,
    val phoneViewSide: PhoneViewSide = PhoneViewSide.CENTER,
    /** How many times the game has pointed out the auto-complete button; it stops after the first few. */
    val autoCompleteTipsShown: Int = 0,
    /** Where the next Easy or Normal deal is taken from its list (`DealPicker`); null until the first one. */
    val dealCursor: Long? = null,
)
