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

data class RollingWindowEvaluation(
    val requiredScore: Double,
    val achievement: Double,
    val averageScore: Double,
    val remainingScore: Double,
)

fun hasRollingWindowData(scores: List<Double?>): Boolean = scores.any { it != null }

// otherDaysScores excludes the selected date; the selected date itself is always counted
// (as selectedDateScore), even when it has no recorded row, so that "achievement reaches
// 100%" and "the 7-day average is at least DAILY_SCORE_TARGET" stay mathematically equivalent.
fun evaluateRollingWindow(
    otherDaysScores: List<Double>,
    selectedDateScore: Double,
): RollingWindowEvaluation {
    val dataDaysCount = otherDaysScores.size + 1
    val otherDaysScoreSum = otherDaysScores.sum()
    val requiredScore = DAILY_SCORE_TARGET * dataDaysCount - otherDaysScoreSum
    val achievement = if (requiredScore <= 0.0) MAX_ACHIEVEMENT else selectedDateScore / requiredScore
    val averageScore = (otherDaysScoreSum + selectedDateScore) / dataDaysCount
    val remainingScore = requiredScore - selectedDateScore
    return RollingWindowEvaluation(requiredScore, achievement, averageScore, remainingScore)
}
