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

internal fun idealWeightAt(
    date: LocalDate,
    startDate: LocalDate,
    deadline: LocalDate,
    baselineKg: Double,
    targetKg: Double,
): Double {
    val totalDays = ChronoUnit.DAYS.between(startDate, deadline)
    if (totalDays <= 0) return targetKg
    val elapsedDays = ChronoUnit.DAYS.between(startDate, date).coerceIn(0, totalDays)
    val fraction = elapsedDays.toDouble() / totalDays
    return baselineKg + (targetKg - baselineKg) * fraction
}

fun idealWeightOnDate(date: LocalDate): Double =
    idealWeightAt(
        date = date,
        startDate = WEIGHT_START_DATE,
        deadline = WEIGHT_DEADLINE,
        baselineKg = WEIGHT_BASELINE_KG,
        targetKg = WEIGHT_TARGET_KG,
    )
