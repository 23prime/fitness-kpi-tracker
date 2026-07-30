package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.WeightPoint
import com.okkey.fitnesskpitracker.domain.WEIGHT_BASELINE_KG
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_START_DATE
import com.okkey.fitnesskpitracker.domain.WEIGHT_TARGET_KG
import com.okkey.fitnesskpitracker.domain.idealWeightOnDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val COLOR_IDEAL_LINE = 0xFF4CAF50L
private val WEIGHT_CHART_HEIGHT = 160.dp
private val WEIGHT_CHART_ACTUAL_STROKE_WIDTH = 3.dp
private val WEIGHT_CHART_IDEAL_STROKE_WIDTH = 2.dp
private val WEIGHT_CHART_IDEAL_DASH_ON = 8.dp
private val WEIGHT_CHART_IDEAL_DASH_OFF = 6.dp
private const val WEIGHT_CHART_IDEAL_ALPHA = 0.5f
private val WEIGHT_CHART_POINT_RADIUS = 4.dp
private const val WEIGHT_CHART_Y_MARGIN_KG = 0.3
private const val WEIGHT_CHART_GRID_DIVISIONS = 4
private val WEIGHT_CHART_GRID_STROKE_WIDTH = 1.dp
private const val WEIGHT_CHART_GRID_ALPHA = 0.3f
private const val WEIGHT_CHART_AXIS_ALPHA = 0.6f

@Composable
internal fun WeightGoalSection(uiState: DashboardUiState) {
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
            if (uiState.isWeightOverdue) {
                Text(
                    stringResource(R.string.dashboard_weight_overdue),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        WeightLineChart(uiState.weightHistory)
        LabeledValueRow(stringResource(R.string.dashboard_label_days_remaining), uiState.daysUntilDeadline.toString())
    }
}

@Composable
private fun WeightLineChart(weightHistory: List<WeightPoint>) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val weights = weightHistory.map { it.weightKg } + listOf(WEIGHT_BASELINE_KG, WEIGHT_TARGET_KG)
    val minWeight = weights.min() - WEIGHT_CHART_Y_MARGIN_KG
    val maxWeight = weights.max() + WEIGHT_CHART_Y_MARGIN_KG

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WeightChartYAxisLabels(minWeight = minWeight, maxWeight = maxWeight, labelColor = labelColor)
            WeightChartCanvas(
                weightHistory = weightHistory,
                minWeight = minWeight,
                maxWeight = maxWeight,
                gridColor = labelColor,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                WEIGHT_START_DATE.format(DATE_DISPLAY_FORMATTER),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
            Text(
                WEIGHT_DEADLINE.format(DATE_DISPLAY_FORMATTER),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun WeightChartYAxisLabels(
    minWeight: Double,
    maxWeight: Double,
    labelColor: Color,
) {
    Column(
        modifier = Modifier.height(WEIGHT_CHART_HEIGHT),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        for (division in 0..WEIGHT_CHART_GRID_DIVISIONS) {
            val weight = maxWeight - (maxWeight - minWeight) * division / WEIGHT_CHART_GRID_DIVISIONS
            Text(formatWeight(weight), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

@Composable
private fun RowScope.WeightChartCanvas(
    weightHistory: List<WeightPoint>,
    minWeight: Double,
    maxWeight: Double,
    gridColor: Color,
) {
    val idealColor = Color(COLOR_IDEAL_LINE)
    val actualColor = MaterialTheme.colorScheme.primary
    val totalDays = ChronoUnit.DAYS.between(WEIGHT_START_DATE, WEIGHT_DEADLINE).coerceAtLeast(1)

    Canvas(
        modifier =
            Modifier
                .weight(1f)
                .height(WEIGHT_CHART_HEIGHT),
    ) {
        fun xFor(date: LocalDate): Float {
            val elapsed = ChronoUnit.DAYS.between(WEIGHT_START_DATE, date).coerceIn(0, totalDays)
            return size.width * (elapsed.toFloat() / totalDays)
        }

        fun yFor(weightKg: Double): Float {
            val fraction = (weightKg - minWeight) / (maxWeight - minWeight)
            return size.height * (1f - fraction.toFloat())
        }

        drawWeightChartXAxis(gridColor)

        drawLine(
            color = idealColor,
            start = Offset(xFor(WEIGHT_START_DATE), yFor(idealWeightOnDate(WEIGHT_START_DATE))),
            end = Offset(xFor(WEIGHT_DEADLINE), yFor(idealWeightOnDate(WEIGHT_DEADLINE))),
            strokeWidth = WEIGHT_CHART_IDEAL_STROKE_WIDTH.toPx(),
            pathEffect =
                PathEffect.dashPathEffect(
                    floatArrayOf(WEIGHT_CHART_IDEAL_DASH_ON.toPx(), WEIGHT_CHART_IDEAL_DASH_OFF.toPx()),
                ),
            alpha = WEIGHT_CHART_IDEAL_ALPHA,
        )

        if (weightHistory.isNotEmpty()) {
            val path =
                Path().apply {
                    val first = weightHistory.first()
                    moveTo(xFor(first.date), yFor(first.weightKg))
                    weightHistory.drop(1).forEach { point -> lineTo(xFor(point.date), yFor(point.weightKg)) }
                }
            drawPath(
                path,
                color = actualColor,
                style = Stroke(width = WEIGHT_CHART_ACTUAL_STROKE_WIDTH.toPx(), cap = StrokeCap.Round),
            )
            weightHistory.forEach { point ->
                drawCircle(
                    color = actualColor,
                    radius = WEIGHT_CHART_POINT_RADIUS.toPx(),
                    center = Offset(xFor(point.date), yFor(point.weightKg)),
                )
            }
        }
    }
}

private fun DrawScope.drawWeightChartXAxis(gridColor: Color) {
    for (division in 1 until WEIGHT_CHART_GRID_DIVISIONS) {
        val y = size.height * division / WEIGHT_CHART_GRID_DIVISIONS
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = WEIGHT_CHART_GRID_STROKE_WIDTH.toPx(),
            alpha = WEIGHT_CHART_GRID_ALPHA,
        )
    }
    drawLine(
        color = gridColor,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = WEIGHT_CHART_GRID_STROKE_WIDTH.toPx(),
        alpha = WEIGHT_CHART_AXIS_ALPHA,
    )
    drawLine(
        color = gridColor,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = WEIGHT_CHART_GRID_STROKE_WIDTH.toPx(),
        alpha = WEIGHT_CHART_AXIS_ALPHA,
    )
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

private fun formatWeight(value: Double): String = String.format(Locale.ROOT, "%.1f kg", value)
