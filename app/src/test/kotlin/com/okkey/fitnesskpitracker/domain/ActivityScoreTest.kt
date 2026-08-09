package com.okkey.fitnesskpitracker.domain

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun isActivityScoreAchieved_belowTarget_isFalse() {
        assertFalse(isActivityScoreAchieved(0.99))
    }

    @Test
    fun isActivityScoreAchieved_exactlyAtTarget_isTrue() {
        assertTrue(isActivityScoreAchieved(1.0))
    }

    @Test
    fun isActivityScoreAchieved_aboveTarget_isTrue() {
        assertTrue(isActivityScoreAchieved(1.5))
    }

    @Test
    fun activityScoreArcSweepDegrees_zero_isZeroDegrees() {
        assertEquals(0f, activityScoreArcSweepDegrees(0.0))
    }

    @Test
    fun activityScoreArcSweepDegrees_halfway_isHalfCircle() {
        assertEquals(180f, activityScoreArcSweepDegrees(0.5))
    }

    @Test
    fun activityScoreArcSweepDegrees_exactlyAtTarget_isFullCircle() {
        assertEquals(360f, activityScoreArcSweepDegrees(1.0))
    }

    @Test
    fun activityScoreArcSweepDegrees_aboveTarget_isCappedAtFullCircle() {
        assertEquals(360f, activityScoreArcSweepDegrees(1.5))
    }

    @Test
    fun activityScoreHistoryWindowStart_returnsSixDaysBeforeEndDate() {
        val start = activityScoreHistoryWindowStart(LocalDate.parse("2026-08-09"))

        assertEquals(LocalDate.parse("2026-08-03"), start)
    }

    @Test
    fun activityScoreChartUpperBound_belowTarget_returnsTarget() {
        val upperBound = activityScoreChartUpperBound(listOf(50.0, 100.0))

        assertEquals(DAILY_SCORE_TARGET, upperBound)
    }

    @Test
    fun activityScoreChartUpperBound_aboveTarget_returnsMaxScore() {
        val upperBound = activityScoreChartUpperBound(listOf(50.0, 200.0))

        assertEquals(200.0, upperBound)
    }

    @Test
    fun activityScoreChartUpperBound_emptyList_returnsTarget() {
        val upperBound = activityScoreChartUpperBound(emptyList())

        assertEquals(DAILY_SCORE_TARGET, upperBound)
    }

    @Test
    fun hasRollingWindowData_allNull_isFalse() {
        assertFalse(hasRollingWindowData(listOf(null, null, null)))
    }

    @Test
    fun hasRollingWindowData_someNonNull_isTrue() {
        assertTrue(hasRollingWindowData(listOf(null, 50.0, null)))
    }

    @Test
    fun evaluateRollingWindow_noOtherDays_requiredScoreIsTargetForSelectedDayAlone() {
        val evaluation = evaluateRollingWindow(otherDaysScores = emptyList(), selectedDateScore = 225.0)

        assertEquals(150.0, evaluation.requiredScore)
        assertEquals(1.5, evaluation.achievement)
        assertEquals(225.0, evaluation.averageScore)
        assertEquals(-75.0, evaluation.remainingScore)
    }

    @Test
    fun evaluateRollingWindow_otherDaysBelowTarget_requiredScoreCoversShortfall() {
        val evaluation =
            evaluateRollingWindow(otherDaysScores = listOf(125.0, 125.0, 125.0, 125.0), selectedDateScore = 80.0)

        assertEquals(250.0, evaluation.requiredScore)
        assertEquals(80.0 / 250.0, evaluation.achievement)
        assertEquals(116.0, evaluation.averageScore)
        assertEquals(170.0, evaluation.remainingScore)
    }

    @Test
    fun evaluateRollingWindow_otherDaysAlreadyAboveTarget_requiredScoreIsZeroOrLessAndAchievementIsFull() {
        val evaluation = evaluateRollingWindow(otherDaysScores = listOf(900.0, 900.0), selectedDateScore = 0.0)

        assertTrue(evaluation.requiredScore <= 0.0)
        assertEquals(1.0, evaluation.achievement)
    }
}
