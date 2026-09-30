package io.github.vinaooo.solo.domain.scoring

import io.github.vinaooo.solo.domain.model.DrawMode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class StandardScoringTest {

    private val scoring = StandardScoring()

    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("scoringTable")
    fun `standard scoring table`(event: ScoreEvent, points: Int) {
        scoring.pointsFor(event) shouldBe points
    }

    companion object {
        @JvmStatic
        fun scoringTable() = listOf(
            Arguments.of(ScoreEvent.WasteToTableau, 5),
            Arguments.of(ScoreEvent.WasteToFoundation, 10),
            Arguments.of(ScoreEvent.TableauToFoundation, 10),
            Arguments.of(ScoreEvent.CardRevealed, 5),
            Arguments.of(ScoreEvent.FoundationToTableau, -15),
            Arguments.of(ScoreEvent.Undo, -15),
            Arguments.of(ScoreEvent.Recycle(DrawMode.ONE, recycleNumber = 1), -100),
            Arguments.of(ScoreEvent.Recycle(DrawMode.ONE, recycleNumber = 5), -100),
            // Draw 3: the first three passes are free; each recycle after the 3rd pass costs 20.
            Arguments.of(ScoreEvent.Recycle(DrawMode.THREE, recycleNumber = 1), 0),
            Arguments.of(ScoreEvent.Recycle(DrawMode.THREE, recycleNumber = 2), 0),
            Arguments.of(ScoreEvent.Recycle(DrawMode.THREE, recycleNumber = 3), -20),
            Arguments.of(ScoreEvent.Recycle(DrawMode.THREE, recycleNumber = 4), -20),
            Arguments.of(ScoreEvent.TimeElapsed(tenSecondPeriods = 1), -2),
            Arguments.of(ScoreEvent.TimeElapsed(tenSecondPeriods = 3), -6),
            Arguments.of(ScoreEvent.Won(elapsedSeconds = 100), 7_000),
            Arguments.of(ScoreEvent.Won(elapsedSeconds = 30), 23_333),
            Arguments.of(ScoreEvent.Won(elapsedSeconds = 29), 0),
        )
    }
}
