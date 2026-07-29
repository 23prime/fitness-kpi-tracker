package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.domain.CYCLING_KM_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.DAILY_SCORE_TARGET
import com.okkey.fitnesskpitracker.domain.STEPS_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_TARGET_KG
import com.okkey.fitnesskpitracker.domain.WORKOUT_SET_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.activityScoreArcSweepDegrees
import com.okkey.fitnesskpitracker.domain.isActivityScoreAchieved
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private const val PERCENT_SCALE = 100
private const val PROGRESS_MIN = 0.0
private const val PROGRESS_MAX = 1.0
private const val COLOR_ACHIEVED = 0xFF81C784L
private const val DONUT_TRACK_SWEEP_DEGREES = 360f
private const val DONUT_START_ANGLE_DEGREES = -90f
private val DONUT_SIZE = 160.dp
private val DONUT_STROKE_WIDTH = 16.dp

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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            stringResource(R.string.dashboard_section_activity),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityScoreDonutChart(
                score = uiState.activityScore,
                achievement = uiState.activityAchievement,
            )
            ActivityScoreBreakdown(
                steps = uiState.steps,
                cyclingDistanceKm = uiState.cyclingDistanceKm,
                workoutSets = uiState.workoutSets,
            )
        }
    }
}

@Composable
private fun ActivityScoreBreakdown(
    steps: Long?,
    cyclingDistanceKm: Double?,
    workoutSets: Int?,
) {
    val stepsValue = steps ?: 0L
    val cyclingValue = cyclingDistanceKm ?: 0.0
    val workoutSetsValue = workoutSets ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            formatBreakdown("%,d 歩", stepsValue, stepsValue * STEPS_COEFFICIENT),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            formatBreakdown("%.1f km", cyclingValue, cyclingValue * CYCLING_KM_COEFFICIENT),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            formatBreakdown("%d セット", workoutSetsValue, workoutSetsValue * WORKOUT_SET_COEFFICIENT),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActivityScoreDonutChart(
    score: Double,
    achievement: Double,
) {
    val achieved = isActivityScoreAchieved(achievement)
    val arcColor = if (achieved) Color(COLOR_ACHIEVED) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sweepAngle = activityScoreArcSweepDegrees(achievement)
    val percentText = formatPercent(achievement)
    val scoreText = "${formatNumber(score)} / ${formatNumber(DAILY_SCORE_TARGET)} pt"
    val chartDescription = stringResource(R.string.dashboard_activity_chart_description, percentText)

    Box(
        modifier =
            Modifier
                .size(DONUT_SIZE)
                .semantics { contentDescription = chartDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = DONUT_STROKE_WIDTH.toPx()
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            val inset = strokeWidthPx / 2
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            drawArc(
                color = trackColor,
                startAngle = DONUT_START_ANGLE_DEGREES,
                sweepAngle = DONUT_TRACK_SWEEP_DEGREES,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = arcColor,
                startAngle = DONUT_START_ANGLE_DEGREES,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(percentText, style = MaterialTheme.typography.headlineMedium)
                if (achieved) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.dashboard_activity_chart_achieved),
                        tint = arcColor,
                    )
                }
            }
            Text(scoreText, style = MaterialTheme.typography.bodySmall)
        }
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

private fun formatBreakdown(
    valueFormat: String,
    value: Number,
    points: Double,
): String = String.format(Locale.ROOT, "$valueFormat（%.1f pt）", value, points)
