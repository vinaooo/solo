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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.solo.core.ui.formatElapsed
import io.github.vinaooo.solo.domain.model.DrawMode
import io.github.vinaooo.solo.domain.model.GameStats
import io.github.vinaooo.solo.domain.model.ScoreRecord
import java.text.DateFormat
import java.util.Date

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(uiState, onBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(uiState: ScoresUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreRow(rank: Int, record: ScoreRecord) {
    val drawMode = stringResource(if (record.drawMode == DrawMode.ONE) R.string.draw_one else R.string.draw_three)
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(record.playedAtMillis))
    ListItem(
        leadingContent = { Text("#$rank", style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(stringResource(R.string.score_details, record.moves, drawMode, date)) },
        trailingContent = { Text(formatElapsed(record.elapsedSeconds), style = MaterialTheme.typography.titleMedium) },
    ) {
        Text(record.points.toString(), fontWeight = FontWeight.Bold)
    }
}
