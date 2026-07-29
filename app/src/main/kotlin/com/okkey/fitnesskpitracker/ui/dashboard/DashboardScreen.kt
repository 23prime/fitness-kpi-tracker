package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_TARGET_KG
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private const val PERCENT_SCALE = 100
private const val PROGRESS_MIN = 0.0
private const val PROGRESS_MAX = 1.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = viewModel::onReload) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.dashboard_button_reload),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ActivityScoreSection(uiState)
            WeightGoalSection(uiState)
        }
    }
}

@Composable
private fun ActivityScoreSection(uiState: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_section_activity), style = MaterialTheme.typography.titleMedium)
        LabeledValueRow(stringResource(R.string.dashboard_label_score), formatNumber(uiState.activityScore))
        LabeledValueRow(
            stringResource(R.string.dashboard_label_achievement),
            formatPercent(uiState.activityAchievement),
        )
        LinearProgressIndicator(
            progress = { uiState.activityAchievement.coerceIn(PROGRESS_MIN, PROGRESS_MAX).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WeightGoalSection(uiState: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_section_weight), style = MaterialTheme.typography.titleMedium)
        val currentWeightKg = uiState.currentWeightKg
        if (currentWeightKg == null) {
            Text(stringResource(R.string.dashboard_weight_no_record))
        } else {
            LabeledValueRow(stringResource(R.string.dashboard_label_current_weight), formatWeight(currentWeightKg))
            LabeledValueRow(stringResource(R.string.dashboard_label_target_weight), formatWeight(WEIGHT_TARGET_KG))
            LabeledValueRow(
                stringResource(R.string.dashboard_label_progress),
                formatPercent(uiState.weightProgress ?: 0.0),
            )
            LinearProgressIndicator(
                progress = { (uiState.weightProgress ?: 0.0).coerceIn(PROGRESS_MIN, PROGRESS_MAX).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (uiState.isWeightOverdue) {
                Text(
                    stringResource(R.string.dashboard_weight_overdue),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        LabeledValueRow(
            stringResource(R.string.dashboard_label_deadline),
            WEIGHT_DEADLINE.format(DATE_DISPLAY_FORMATTER),
        )
        LabeledValueRow(stringResource(R.string.dashboard_label_days_remaining), uiState.daysUntilDeadline.toString())
    }
}

@Composable
private fun LabeledValueRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value)
    }
}

private fun formatNumber(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

private fun formatWeight(value: Double): String = String.format(Locale.ROOT, "%.1f kg", value)

private fun formatPercent(value: Double): String = "${(value * PERCENT_SCALE).roundToInt()}%"
