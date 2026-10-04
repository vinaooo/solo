package io.github.vinaooo.solo.domain.model

/** How a game is scored and limited. Each game keeps the mode it was dealt with. */
enum class GameMode {
    /** Windows Standard scoring: points, a time penalty and a speed bonus. */
    STANDARD,

    /** Vegas: the game costs $52 and each card on a foundation earns $5; limited passes through the stock. */
    VEGAS,

    /** Vegas whose balance carries over from game to game. */
    VEGAS_CUMULATIVE,

    /** A time limit to win in; the faster the win, the better. */
    COUNTER_TIME,
    ;

    val isVegas: Boolean get() = this == VEGAS || this == VEGAS_CUMULATIVE

    /** How many times the waste may go back to the stock, or null for no limit: one pass in Draw 1, three in Draw 3. */
    fun recycleLimit(drawMode: DrawMode): Int? = if (isVegas) {
        when (drawMode) {
            DrawMode.ONE -> 0
            DrawMode.THREE -> VEGAS_DRAW_THREE_RECYCLES
        }
    } else {
        null
    }

    /** The time to win in, or null for none: 10 minutes in Draw 1, 15 in Draw 3, as chosen with the user. */
    fun timeLimitSeconds(drawMode: DrawMode): Long? = if (this == COUNTER_TIME) {
        when (drawMode) {
            DrawMode.ONE -> DRAW_ONE_LIMIT_SECONDS
            DrawMode.THREE -> DRAW_THREE_LIMIT_SECONDS
        }
    } else {
        null
    }

    /** The score a new game starts with: Vegas pays its $52 up front, from the carried [bank] when cumulative. */
    fun startingScore(bank: Int): Int = when (this) {
        VEGAS -> -VEGAS_STAKE
        VEGAS_CUMULATIVE -> bank - VEGAS_STAKE
        STANDARD, COUNTER_TIME -> 0
    }

    private companion object {
        const val VEGAS_DRAW_THREE_RECYCLES = 2
        const val VEGAS_STAKE = 52
        const val DRAW_ONE_LIMIT_SECONDS = 10 * 60L
        const val DRAW_THREE_LIMIT_SECONDS = 15 * 60L
    }
}
