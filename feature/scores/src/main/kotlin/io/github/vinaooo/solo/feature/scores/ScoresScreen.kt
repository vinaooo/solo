package io.github.vinaooo.solo.feature.scores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.ui.formatDollars
import io.github.vinaooo.solo.core.ui.label
import io.github.vinaooo.solo.core.ui.spokenDollars
import io.github.vinaooo.solo.domain.model.Difficulty
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import io.github.vinaooo.vinkit.core.formatElapsed
import io.github.vinaooo.vinkit.designsystem.spokenElapsed
import java.text.DateFormat
import java.util.Date

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(uiState, onBack, modifier, onSelectMode = viewModel::selectMode)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(
    uiState: ScoresUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectMode: (GameMode) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scores_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { StatsCard(uiState.stats) }
            // One ranking per mode already played; a single one needs no tabs.
            if (uiState.modes.size > 1) {
                item {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = uiState.modes.indexOf(uiState.mode).coerceAtLeast(0),
                        edgePadding = 0.dp,
                    ) {
                        uiState.modes.forEach { mode ->
                            Tab(
                                selected = mode == uiState.mode,
                                onClick = { onSelectMode(mode) },
                                text = { Text(stringResource(mode.label)) },
                            )
                        }
                    }
                }
            }
            if (!uiState.isLoading && uiState.scores.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_scores), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            itemsIndexed(uiState.scores) { index, record -> ScoreRow(index + 1, record) }
        }
    }
}

@Composable
private fun StatsCard(stats: GameStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(stringResource(R.string.stat_played), stats.played.toString())
            StatItem(stringResource(R.string.stat_won), stats.won.toString())
            StatItem(stringResource(R.string.stat_win_rate), "${stats.winRatePercent}%")
            StatItem(stringResource(R.string.stat_streak), stats.currentStreak.toString())
            StatItem(stringResource(R.string.stat_best_streak), stats.bestStreak.toString())
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, $value" },
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreRow(rank: Int, record: ScoreRecord) {
    val drawMode = stringResource(if (record.drawMode == DrawMode.ONE) R.string.draw_one else R.string.draw_three)
    val difficulty = stringResource(
        when (record.difficulty) {
            Difficulty.EASY -> R.string.difficulty_easy
            Difficulty.NORMAL -> R.string.difficulty_normal
            Difficulty.HARD -> R.string.difficulty_hard
        },
    )
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(record.playedAtMillis))
    val rankDescription = stringResource(R.string.rank_description, rank)
    val spokenTime = spokenElapsed(record.elapsedSeconds)
    val spokenMoney = spokenDollars(record.points)
    ListItem(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        leadingContent = {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = rankDescription },
            )
        },
        supportingContent = { Text(stringResource(R.string.score_details, record.moves, drawMode, difficulty, date)) },
        // Counter time ranks by time, so the time is the headline and there are no points to show.
        trailingContent = if (record.mode == GameMode.COUNTER_TIME) {
            null
        } else {
            {
                Text(
                    formatElapsed(record.elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { contentDescription = spokenTime },
                )
            }
        },
    ) {
        when {
            record.mode.isVegas -> Text(
                formatDollars(record.points),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = spokenMoney },
            )
            record.mode == GameMode.COUNTER_TIME -> Text(
                formatElapsed(record.elapsedSeconds),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = spokenTime },
            )
            else -> Text(record.points.toString(), fontWeight = FontWeight.Bold)
        }
    }
}
