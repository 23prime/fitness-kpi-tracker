package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.domain.CYCLING_KM_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.DAILY_SCORE_TARGET
import com.okkey.fitnesskpitracker.domain.STEPS_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.WORKOUT_SET_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.activityScoreArcSweepDegrees
import com.okkey.fitnesskpitracker.domain.isActivityScoreAchieved
import java.time.LocalDate
import java.util.Locale

private const val COLOR_ACHIEVED = 0xFF81C784L
private const val DONUT_TRACK_SWEEP_DEGREES = 360f
private const val DONUT_START_ANGLE_DEGREES = -90f
private val DONUT_SIZE = 160.dp
private val DONUT_STROKE_WIDTH = 16.dp
private val SWIPE_THRESHOLD = 96.dp
private val BREAKDOWN_ICON_SIZE = 20.dp
private val BREAKDOWN_ICON_SPACING = 8.dp

@Composable
internal fun ActivityScoreSection(
    uiState: DashboardUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityScoreDonutChart(
                date = uiState.date,
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
private fun ActivityScoreBreakdown(
    steps: Long?,
    cyclingDistanceKm: Double?,
    workoutSets: Int?,
) {
    val stepsValue = steps ?: 0L
    val cyclingValue = cyclingDistanceKm ?: 0.0
    val workoutSetsValue = workoutSets ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BreakdownRow(
            iconRes = R.drawable.ic_directions_walk,
            text = formatBreakdown("%,d 歩", stepsValue, stepsValue * STEPS_COEFFICIENT),
            descriptionRes = R.string.dashboard_breakdown_steps_description,
        )
        BreakdownRow(
            iconRes = R.drawable.ic_directions_bike,
            text = formatBreakdown("%.1f km", cyclingValue, cyclingValue * CYCLING_KM_COEFFICIENT),
            descriptionRes = R.string.dashboard_breakdown_cycling_description,
        )
        BreakdownRow(
            iconRes = R.drawable.ic_fitness_center,
            text = formatBreakdown("%d セット", workoutSetsValue, workoutSetsValue * WORKOUT_SET_COEFFICIENT),
            descriptionRes = R.string.dashboard_breakdown_workout_description,
        )
    }
}

@Composable
private fun BreakdownRow(
    @DrawableRes iconRes: Int,
    text: String,
    @StringRes descriptionRes: Int,
) {
    val description = stringResource(descriptionRes, text)
    Row(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalArrangement = Arrangement.spacedBy(BREAKDOWN_ICON_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(BREAKDOWN_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActivityScoreDonutChart(
    date: LocalDate,
    score: Double,
    achievement: Double,
) {
    val achieved = isActivityScoreAchieved(achievement)
    val arcColor = if (achieved) Color(COLOR_ACHIEVED) else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sweepAngle = activityScoreArcSweepDegrees(achievement)
    val percentText = formatPercent(achievement)
    val scoreText = "${formatNumber(score)} / ${formatNumber(DAILY_SCORE_TARGET)} pt"
    val chartDescription =
        stringResource(
            R.string.dashboard_activity_chart_description,
            date.format(DATE_DISPLAY_FORMATTER),
            percentText,
        )

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

private fun formatNumber(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

private fun formatBreakdown(
    valueFormat: String,
    value: Number,
    points: Double,
): String = String.format(Locale.ROOT, "$valueFormat（%.1f pt）", value, points)
