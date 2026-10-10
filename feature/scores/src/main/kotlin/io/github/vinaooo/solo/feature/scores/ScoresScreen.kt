package io.github.vinaooo.solo.feature.scores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.ui.formatDollars
import io.github.vinaooo.solo.core.ui.label
import io.github.vinaooo.solo.core.ui.spokenDollars
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.difficulty
import io.github.vinaooo.solo.domain.model.drawMode
import io.github.vinaooo.solo.domain.model.gameMode
import io.github.vinaooo.solo.domain.model.moves
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.designsystem.R as DesignR
import io.github.vinaooo.vinkit.scores.ScorePoints
import io.github.vinaooo.vinkit.scores.ScoresScreen as VinkitScoresScreen
import io.github.vinaooo.vinkit.scores.ScoresUiState

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SoloScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bank by viewModel.bank.collectAsStateWithLifecycle()
    ScoresScreen(uiState, onBack, modifier, bank, viewModel::selectGroup)
}

/**
 * vinkit's Scores screen in Solo's words: Vegas in dollars, cumulative Vegas's balance ([bank]) under its stats, and
 * each score's moves, draw mode and difficulty.
 */
@Composable
fun ScoresScreen(
    uiState: ScoresUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bank: Int = 0,
    onSelectMode: (String) -> Unit = {},
) {
    VinkitScoresScreen(
        uiState = uiState,
        onBack = onBack,
        modeName = { stringResource(enumValueOf<GameMode>(it).label) },
        modifier = modifier,
        onSelectGroup = onSelectMode,
        details = { details(it) },
        points = { points(it) },
        note = {
            if (it ==
                GameMode.VEGAS_CUMULATIVE.name
            ) {
                stringResource(R.string.vegas_balance, formatDollars(bank))
            } else {
                null
            }
        },
    )
}

@Composable
private fun details(record: ScoreRecord): String = stringResource(
    R.string.score_details,
    record.moves,
    stringResource(if (record.drawMode == DrawMode.ONE) R.string.draw_one else R.string.draw_three),
    stringResource(
        when (record.difficulty) {
            Difficulty.EASY -> DesignR.string.vinkit_difficulty_easy
            Difficulty.NORMAL -> DesignR.string.vinkit_difficulty_medium
            Difficulty.HARD -> DesignR.string.vinkit_difficulty_hard
        },
    ),
)

@Composable
private fun points(record: ScoreRecord): ScorePoints = if (record.gameMode.isVegas) {
    ScorePoints(formatDollars(record.points), spokenDollars(record.points))
} else {
    ScorePoints(record.points.toString())
}
