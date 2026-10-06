package io.github.vinaooo.solo.domain.model

data class ScoreRecord(
    val points: Int,
    val elapsedSeconds: Long,
    val moves: Int,
    val drawMode: DrawMode,
    val playedAtMillis: Long,
    val mode: GameMode = GameMode.STANDARD,
    val difficulty: Difficulty = Difficulty.HARD,
) {
    companion object {
        /** Highest points (or dollars) first; ties go to the fastest game. */
        val RANKING: Comparator<ScoreRecord> =
            compareByDescending<ScoreRecord> { it.points }.thenBy { it.elapsedSeconds }

        /** Counter time: the fastest win first; ties go to fewer moves. */
        val FASTEST: Comparator<ScoreRecord> = compareBy<ScoreRecord> { it.elapsedSeconds }.thenBy { it.moves }

        fun rankingFor(mode: GameMode): Comparator<ScoreRecord> =
            if (mode == GameMode.COUNTER_TIME) FASTEST else RANKING

        /** The modes that keep a ranking: cumulative Vegas only shows its balance, by the user's choice. */
        fun isRanked(mode: GameMode): Boolean = mode != GameMode.VEGAS_CUMULATIVE

        const val TOP_LIMIT = 10
    }
}
