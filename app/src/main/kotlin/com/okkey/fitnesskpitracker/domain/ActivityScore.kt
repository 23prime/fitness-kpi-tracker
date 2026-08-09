package com.okkey.fitnesskpitracker.domain

import java.time.LocalDate

private const val MIN_ACHIEVEMENT = 0.0
private const val MAX_ACHIEVEMENT = 1.0
private const val FULL_CIRCLE_DEGREES = 360.0
private const val HISTORY_WINDOW_DAYS = 7L

fun activityScore(
    steps: Long?,
    cyclingDistanceKm: Double?,
    workoutSets: Int?,
): Double =
    (steps ?: 0) * STEPS_COEFFICIENT +
        (cyclingDistanceKm ?: 0.0) * CYCLING_KM_COEFFICIENT +
        (workoutSets ?: 0) * WORKOUT_SET_COEFFICIENT

fun dailyScoreAchievement(score: Double): Double = score / DAILY_SCORE_TARGET

fun isActivityScoreAchieved(achievement: Double): Boolean = achievement >= MAX_ACHIEVEMENT

fun activityScoreArcSweepDegrees(achievement: Double): Float {
    val clamped = achievement.coerceIn(MIN_ACHIEVEMENT, MAX_ACHIEVEMENT)
    return (clamped * FULL_CIRCLE_DEGREES).toFloat()
}

fun activityScoreHistoryWindowStart(endDate: LocalDate): LocalDate = endDate.minusDays(HISTORY_WINDOW_DAYS - 1)

fun activityScoreChartUpperBound(scores: List<Double>): Double {
    val maxScore = scores.maxOrNull() ?: 0.0
    return maxScore.coerceAtLeast(DAILY_SCORE_TARGET)
}
