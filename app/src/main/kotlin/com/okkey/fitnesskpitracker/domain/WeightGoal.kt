package com.okkey.fitnesskpitracker.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun goalProgress(
    baselineKg: Double,
    targetKg: Double,
    currentKg: Double,
): Double {
    if (baselineKg == targetKg) return 1.0
    return (baselineKg - currentKg) / (baselineKg - targetKg)
}

fun weightGoalProgress(currentWeightKg: Double): Double =
    goalProgress(
        baselineKg = WEIGHT_BASELINE_KG,
        targetKg = WEIGHT_TARGET_KG,
        currentKg = currentWeightKg,
    )

fun daysUntilWeightDeadline(today: LocalDate): Long = ChronoUnit.DAYS.between(today, WEIGHT_DEADLINE)

fun isWeightGoalOverdue(
    today: LocalDate,
    progress: Double,
): Boolean = today.isAfter(WEIGHT_DEADLINE) && progress < 1.0
