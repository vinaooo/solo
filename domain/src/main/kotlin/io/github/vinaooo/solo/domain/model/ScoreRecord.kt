package io.github.vinaooo.solo.domain.model

data class ScoreRecord(
    val points: Int,
    val elapsedSeconds: Long,
    val moves: Int,
    val drawMode: DrawMode,
    val playedAtMillis: Long,
) {
    companion object {
        /** Highest points first; ties go to the fastest game. */
        val RANKING: Comparator<ScoreRecord> =
            compareByDescending<ScoreRecord> { it.points }.thenBy { it.elapsedSeconds }

        const val TOP_LIMIT = 10
    }
}
