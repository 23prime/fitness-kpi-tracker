package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.DailyActivityScorePoint
import com.okkey.fitnesskpitracker.domain.ActivityScoreEvaluationMode
import com.okkey.fitnesskpitracker.domain.RollingWindowEvaluation
import com.okkey.fitnesskpitracker.domain.activityScoreArcSweepDegrees
import com.okkey.fitnesskpitracker.domain.isActivityScoreAchieved
import java.time.LocalDate
import java.util.Locale

internal const val COLOR_ACHIEVED = 0xFF81C784L
private const val DONUT_TRACK_SWEEP_DEGREES = 360f
private const val DONUT_START_ANGLE_DEGREES = -90f
private val DONUT_SIZE = 160.dp
private val DONUT_STROKE_WIDTH = 16.dp
private val SWIPE_THRESHOLD = 96.dp
private const val DATE_SWITCH_ANIMATION_DURATION_MS = 300

@Composable
internal fun ActivityScoreSection(
    uiState: DashboardUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onEvaluationModeChange: (ActivityScoreEvaluationMode) -> Unit,
) {
    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .pointerInput(onPreviousDay, onNextDay) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                        onDragEnd = {
                            if (totalDrag <= -swipeThresholdPx) {
                                onNextDay()
                            } else if (totalDrag >= swipeThresholdPx) {
                                onPreviousDay()
                            }
                        },
                    )
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            stringResource(R.string.dashboard_section_activity),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        DateSwitcherRow(
            date = uiState.date,
            canGoToPreviousDay = uiState.canGoToPreviousDay,
            canGoToNextDay = uiState.canGoToNextDay,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
        )
        EvaluationModeToggle(
            mode = uiState.activityScoreEvaluationMode,
            onModeChange = onEvaluationModeChange,
        )
        ActivityScoreContent(uiState = uiState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvaluationModeToggle(
    mode: ActivityScoreEvaluationMode,
    onModeChange: (ActivityScoreEvaluationMode) -> Unit,
) {
    val entries = ActivityScoreEvaluationMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = mode == entry,
                onClick = { onModeChange(entry) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
            ) {
                Text(stringResource(evaluationModeLabel(entry)))
            }
        }
    }
}

private fun evaluationModeLabel(mode: ActivityScoreEvaluationMode): Int =
    when (mode) {
        ActivityScoreEvaluationMode.ROLLING_WINDOW -> R.string.dashboard_activity_evaluation_mode_rolling_window
        ActivityScoreEvaluationMode.DAILY_ONLY -> R.string.dashboard_activity_evaluation_mode_daily_only
    }

@Composable
private fun ActivityScoreContent(uiState: DashboardUiState) {
    AnimatedContent(
        targetState = ActivityScoreSnapshot(uiState),
        transitionSpec = {
            if (initialState.date == targetState.date) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val direction = if (targetState.date.isAfter(initialState.date)) 1 else -1
                (
                    slideInHorizontally(animationSpec = tween(DATE_SWITCH_ANIMATION_DURATION_MS)) { fullWidth ->
                        direction * fullWidth
                    } + fadeIn(animationSpec = tween(DATE_SWITCH_ANIMATION_DURATION_MS))
                ).togetherWith(
                    slideOutHorizontally(animationSpec = tween(DATE_SWITCH_ANIMATION_DURATION_MS)) { fullWidth ->
                        -direction * fullWidth
                    } + fadeOut(animationSpec = tween(DATE_SWITCH_ANIMATION_DURATION_MS)),
                )
            }
        },
    ) { snapshot ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityScoreDonutChart(
                    date = snapshot.date,
                    score = snapshot.activityScore,
                    rollingWindow = snapshot.activityRollingWindow,
                    mode = snapshot.activityScoreEvaluationMode,
                )
                ActivityScoreBreakdown(
                    steps = snapshot.steps,
                    cyclingDistanceKm = snapshot.cyclingDistanceKm,
                    workoutSets = snapshot.workoutSets,
                )
            }
            ActivityScoreHistoryChart(history = snapshot.activityScoreHistory)
            RollingWindowSummary(
                rollingWindow = snapshot.activityRollingWindow,
                isSelectedDateToday = snapshot.isSelectedDateToday,
                mode = snapshot.activityScoreEvaluationMode,
            )
        }
    }
}

/**
 * Snapshot of the fields ActivityScoreContent renders, passed to AnimatedContent as its
 * targetState. Capturing them as one value (rather than reading DashboardUiState directly inside
 * the content lambda) keeps the outgoing slide frozen at the values it started exiting with,
 * instead of jumping to the incoming day's values as soon as the ViewModel updates.
 */
private data class ActivityScoreSnapshot(
    val date: LocalDate,
    val activityScore: Double,
    val activityScoreEvaluationMode: ActivityScoreEvaluationMode,
    val activityRollingWindow: RollingWindowEvaluation?,
    val isSelectedDateToday: Boolean,
    val activityScoreHistory: List<DailyActivityScorePoint>,
    val steps: Long?,
    val cyclingDistanceKm: Double?,
    val workoutSets: Int?,
) {
    constructor(uiState: DashboardUiState) : this(
        date = uiState.date,
        activityScore = uiState.activityScore,
        activityScoreEvaluationMode = uiState.activityScoreEvaluationMode,
        activityRollingWindow = uiState.activityRollingWindow,
        isSelectedDateToday = uiState.isSelectedDateToday,
        activityScoreHistory = uiState.activityScoreHistory,
        steps = uiState.steps,
        cyclingDistanceKm = uiState.cyclingDistanceKm,
        workoutSets = uiState.workoutSets,
    )
}

@Composable
private fun DateSwitcherRow(
    date: LocalDate,
    canGoToPreviousDay: Boolean,
    canGoToNextDay: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousDay, enabled = canGoToPreviousDay) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.dashboard_button_previous_day),
            )
        }
        Text(date.format(DATE_DISPLAY_FORMATTER), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNextDay, enabled = canGoToNextDay) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.dashboard_button_next_day),
            )
        }
    }
}

@Composable
private fun ActivityScoreDonutChart(
    date: LocalDate,
    score: Double,
    rollingWindow: RollingWindowEvaluation?,
    mode: ActivityScoreEvaluationMode,
) {
    if (rollingWindow == null) {
        ActivityScoreDonutNoData(date = date, mode = mode)
        return
    }
    val achieved = isActivityScoreAchieved(rollingWindow.achievement)
    val arcColor = if (achieved) Color(COLOR_ACHIEVED) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sweepAngle = activityScoreArcSweepDegrees(rollingWindow.achievement)
    val percentText = formatPercent(rollingWindow.achievement)
    val scoreText =
        if (rollingWindow.requiredScore <= 0.0) {
            stringResource(R.string.dashboard_activity_maintained)
        } else {
            "${formatNumber(score)} / ${formatNumber(rollingWindow.requiredScore)} pt"
        }
    val chartDescriptionRes =
        when (mode) {
            ActivityScoreEvaluationMode.ROLLING_WINDOW -> R.string.dashboard_activity_chart_description
            ActivityScoreEvaluationMode.DAILY_ONLY -> R.string.dashboard_activity_chart_description_daily_only
        }
    val chartDescription = stringResource(chartDescriptionRes, date.format(DATE_DISPLAY_FORMATTER), percentText)

    Box(
        modifier =
            Modifier
                .size(DONUT_SIZE)
                .semantics(mergeDescendants = true) { contentDescription = chartDescription },
        contentAlignment = Alignment.Center,
    ) {
        DonutArcs(trackColor = trackColor, arcColor = arcColor, sweepAngle = sweepAngle)
        DonutCenterLabel(percentText = percentText, scoreText = scoreText, achieved = achieved, arcColor = arcColor)
    }
}

@Composable
private fun ActivityScoreDonutNoData(
    date: LocalDate,
    mode: ActivityScoreEvaluationMode,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val noDataText = stringResource(R.string.dashboard_activity_no_data)
    val chartNoDataDescriptionRes =
        when (mode) {
            ActivityScoreEvaluationMode.ROLLING_WINDOW -> R.string.dashboard_activity_chart_no_data_description
            ActivityScoreEvaluationMode.DAILY_ONLY -> R.string.dashboard_activity_chart_no_data_description_daily_only
        }
    val chartDescription = stringResource(chartNoDataDescriptionRes, date.format(DATE_DISPLAY_FORMATTER))

    Box(
        modifier =
            Modifier
                .size(DONUT_SIZE)
                .semantics(mergeDescendants = true) { contentDescription = chartDescription },
        contentAlignment = Alignment.Center,
    ) {
        DonutArcs(trackColor = trackColor, arcColor = trackColor, sweepAngle = 0f)
        Text(noDataText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DonutArcs(
    trackColor: Color,
    arcColor: Color,
    sweepAngle: Float,
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
}

@Composable
private fun DonutCenterLabel(
    percentText: String,
    scoreText: String,
    achieved: Boolean,
    arcColor: Color,
) {
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

internal fun formatNumber(value: Double): String = String.format(Locale.ROOT, "%.1f", value)
