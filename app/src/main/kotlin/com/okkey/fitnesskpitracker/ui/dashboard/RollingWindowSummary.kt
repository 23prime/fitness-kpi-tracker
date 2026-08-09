package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.domain.ROLLING_WINDOW_TOTAL_SCORE_TARGET
import com.okkey.fitnesskpitracker.domain.RollingWindowEvaluation
import com.okkey.fitnesskpitracker.domain.isActivityScoreAchieved

private val ROLLING_WINDOW_SUMMARY_SPACING = 8.dp

@Composable
internal fun RollingWindowSummary(
    rollingWindow: RollingWindowEvaluation?,
    isSelectedDateToday: Boolean,
) {
    if (rollingWindow == null) {
        Text(stringResource(R.string.dashboard_activity_no_data), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val averageText =
        stringResource(R.string.dashboard_activity_rolling_average, formatNumber(rollingWindow.averageScore))
    val totalText =
        stringResource(
            R.string.dashboard_activity_rolling_total,
            formatNumber(rollingWindow.totalScore),
            formatNumber(ROLLING_WINDOW_TOTAL_SCORE_TARGET),
        )
    val remainingText =
        if (!isSelectedDateToday) {
            null
        } else if (isActivityScoreAchieved(rollingWindow.achievement)) {
            stringResource(R.string.dashboard_activity_maintained)
        } else {
            stringResource(R.string.dashboard_activity_remaining_score, formatNumber(rollingWindow.remainingScore))
        }
    val description = remainingText?.let { "$averageText $totalText $it" } ?: "$averageText $totalText"
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(ROLLING_WINDOW_SUMMARY_SPACING),
    ) {
        Text(averageText, style = MaterialTheme.typography.bodyMedium)
        Text(totalText, style = MaterialTheme.typography.bodyMedium)
        remainingText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
