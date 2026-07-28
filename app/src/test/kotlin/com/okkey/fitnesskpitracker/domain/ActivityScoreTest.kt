package com.okkey.fitnesskpitracker.domain

import org.junit.Test
import kotlin.test.assertEquals

class ActivityScoreTest {
    @Test
    fun activityScore_stepsAndCyclingAndWorkout_matchesRequirementExample() {
        val score = activityScore(steps = 8_000, cyclingDistanceKm = 10.0, workoutSets = 5)

        assertEquals(260.0, score)
    }

    @Test
    fun activityScore_allNull_isZero() {
        val score = activityScore(steps = null, cyclingDistanceKm = null, workoutSets = null)

        assertEquals(0.0, score)
    }

    @Test
    fun activityScore_stepsNull_countsOnlyCyclingAndWorkout() {
        val score = activityScore(steps = null, cyclingDistanceKm = 10.0, workoutSets = 5)

        assertEquals(100.0, score)
    }

    @Test
    fun dailyScoreAchievement_scoreAtTarget_isOne() {
        val achievement = dailyScoreAchievement(150.0)

        assertEquals(1.0, achievement)
    }

    @Test
    fun dailyScoreAchievement_scoreAtHalfTarget_isHalf() {
        val achievement = dailyScoreAchievement(75.0)

        assertEquals(0.5, achievement)
    }
}
