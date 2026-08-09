package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.DailyActivityScorePoint
import com.okkey.fitnesskpitracker.domain.DAILY_SCORE_TARGET
import com.okkey.fitnesskpitracker.domain.activityScoreChartUpperBound
import com.okkey.fitnesskpitracker.domain.dailyScoreAchievement
import com.okkey.fitnesskpitracker.domain.isActivityScoreAchieved
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private val HISTORY_CHART_HEIGHT = 128.dp
private const val HISTORY_CHART_BAR_WIDTH_RATIO = 0.6f
private const val HISTORY_CHART_AXIS_STEP = 25.0
private val HISTORY_CHART_TOP_PADDING = 16.dp
private val HISTORY_CHART_TARGET_LINE_STROKE_WIDTH = 1.dp
private val HISTORY_CHART_TARGET_LINE_DASH_ON = 6.dp
private val HISTORY_CHART_TARGET_LINE_DASH_OFF = 4.dp
private const val HISTORY_CHART_TARGET_LINE_ALPHA = 0.6f
private const val HISTORY_CHART_GRID_DIVISIONS = 4
private val HISTORY_CHART_GRID_STROKE_WIDTH = 1.dp
private const val HISTORY_CHART_GRID_ALPHA = 0.3f
private const val HISTORY_CHART_AXIS_ALPHA = 0.6f
private val HISTORY_CHART_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")
private val HISTORY_CHART_Y_AXIS_LABEL_SPACING = 4.dp

@Composable
internal fun ActivityScoreHistoryChart(history: List<DailyActivityScorePoint>) {
    if (history.isEmpty()) return
    val achievedColor = Color(COLOR_ACHIEVED)
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    val upperBound = historyChartUpperBound(history.mapNotNull { it.score })
    val chartDescription =
        stringResource(
            R.string.dashboard_activity_history_chart_description,
            history.first().date.format(DATE_DISPLAY_FORMATTER),
            history.last().date.format(DATE_DISPLAY_FORMATTER),
        )

    Row(
        modifier = Modifier.padding(top = HISTORY_CHART_TOP_PADDING),
        horizontalArrangement = Arrangement.spacedBy(HISTORY_CHART_Y_AXIS_LABEL_SPACING),
    ) {
        HistoryChartYAxisLabels(upperBound = upperBound, labelColor = gridColor)
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(HISTORY_CHART_HEIGHT)
                        .semantics(mergeDescendants = true) { contentDescription = chartDescription },
            ) {
                drawHistoryChartGrid(gridColor)
                drawActivityScoreHistoryBars(
                    history = history,
                    upperBound = upperBound,
                    achievedColor = achievedColor,
                    barColor = barColor,
                    targetLineColor = gridColor,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    history.first().date.format(HISTORY_CHART_LABEL_FORMATTER),
                    style = MaterialTheme.typography.labelSmall,
                    color = gridColor,
                )
                Text(
                    history.last().date.format(HISTORY_CHART_LABEL_FORMATTER),
                    style = MaterialTheme.typography.labelSmall,
                    color = gridColor,
                )
            }
        }
    }
}

@Composable
private fun HistoryChartYAxisLabels(
    upperBound: Double,
    labelColor: Color,
) {
    Column(
        modifier = Modifier.height(HISTORY_CHART_HEIGHT),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        for (division in 0..HISTORY_CHART_GRID_DIVISIONS) {
            val score = upperBound - upperBound * division / HISTORY_CHART_GRID_DIVISIONS
            Text(formatHistoryChartScore(score), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

private fun formatHistoryChartScore(score: Double): String = String.format(Locale.ROOT, "%.0f", score)

// Rounds the per-division step up to the nearest HISTORY_CHART_AXIS_STEP so axis labels
// land on round numbers (0/50/100/...) instead of fractions of the raw max score.
private fun historyChartUpperBound(scores: List<Double>): Double {
    val rawUpperBound = activityScoreChartUpperBound(scores)
    val step =
        ceil(rawUpperBound / HISTORY_CHART_GRID_DIVISIONS / HISTORY_CHART_AXIS_STEP) * HISTORY_CHART_AXIS_STEP
    return step * HISTORY_CHART_GRID_DIVISIONS
}

private fun DrawScope.drawHistoryChartGrid(gridColor: Color) {
    for (division in 1 until HISTORY_CHART_GRID_DIVISIONS) {
        val y = size.height * division / HISTORY_CHART_GRID_DIVISIONS
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = HISTORY_CHART_GRID_STROKE_WIDTH.toPx(),
            alpha = HISTORY_CHART_GRID_ALPHA,
        )
    }
    drawLine(
        color = gridColor,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = HISTORY_CHART_GRID_STROKE_WIDTH.toPx(),
        alpha = HISTORY_CHART_AXIS_ALPHA,
    )
    drawLine(
        color = gridColor,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = HISTORY_CHART_GRID_STROKE_WIDTH.toPx(),
        alpha = HISTORY_CHART_AXIS_ALPHA,
    )
}

private fun DrawScope.drawActivityScoreHistoryBars(
    history: List<DailyActivityScorePoint>,
    upperBound: Double,
    achievedColor: Color,
    barColor: Color,
    targetLineColor: Color,
) {
    fun yFor(score: Double): Float = size.height * (1f - (score / upperBound).toFloat())

    drawLine(
        color = targetLineColor,
        start = Offset(0f, yFor(DAILY_SCORE_TARGET)),
        end = Offset(size.width, yFor(DAILY_SCORE_TARGET)),
        strokeWidth = HISTORY_CHART_TARGET_LINE_STROKE_WIDTH.toPx(),
        pathEffect =
            PathEffect.dashPathEffect(
                floatArrayOf(HISTORY_CHART_TARGET_LINE_DASH_ON.toPx(), HISTORY_CHART_TARGET_LINE_DASH_OFF.toPx()),
            ),
        alpha = HISTORY_CHART_TARGET_LINE_ALPHA,
    )

    val slotWidth = size.width / history.size
    val barWidth = slotWidth * HISTORY_CHART_BAR_WIDTH_RATIO
    history.forEachIndexed { index, point ->
        val score = point.score ?: return@forEachIndexed
        val achieved = isActivityScoreAchieved(dailyScoreAchievement(score))
        val top = yFor(score)
        val left = slotWidth * index + (slotWidth - barWidth) / 2
        drawRect(
            color = if (achieved) achievedColor else barColor,
            topLeft = Offset(left, top),
            size = Size(barWidth, size.height - top),
        )
    }
}
