package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.domain.CYCLING_KM_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.STEPS_COEFFICIENT
import com.okkey.fitnesskpitracker.domain.WORKOUT_SET_COEFFICIENT
import java.util.Locale

private val BREAKDOWN_ICON_SIZE = 20.dp
private val BREAKDOWN_ICON_SPACING = 8.dp

@Composable
internal fun ActivityScoreBreakdown(
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

private fun formatBreakdown(
    valueFormat: String,
    value: Number,
    points: Double,
): String = String.format(Locale.ROOT, "$valueFormat（%.1f pt）", value, points)
