package com.okkey.fitnesskpitracker.domain

fun activityScore(
    steps: Long?,
    cyclingDistanceKm: Double?,
    workoutSets: Int?,
): Double =
    (steps ?: 0) * STEPS_COEFFICIENT +
        (cyclingDistanceKm ?: 0.0) * CYCLING_KM_COEFFICIENT +
        (workoutSets ?: 0) * WORKOUT_SET_COEFFICIENT

fun dailyScoreAchievement(score: Double): Double = score / DAILY_SCORE_TARGET
