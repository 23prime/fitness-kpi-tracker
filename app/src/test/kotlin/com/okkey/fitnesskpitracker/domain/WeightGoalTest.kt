package com.okkey.fitnesskpitracker.domain

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeightGoalTest {
    @Test
    fun goalProgress_losingWeightDirection_isNormalizedBetweenZeroAndOne() {
        val progress = goalProgress(baselineKg = 60.0, targetKg = 59.0, currentKg = 59.5)

        assertEquals(0.5, progress)
    }

    @Test
    fun goalProgress_gainingWeightDirection_isNormalizedBetweenZeroAndOne() {
        val progress = goalProgress(baselineKg = 60.0, targetKg = 65.0, currentKg = 62.0)

        assertEquals(0.4, progress)
    }

    @Test
    fun goalProgress_baselineEqualsTarget_isAchieved() {
        val progress = goalProgress(baselineKg = 60.0, targetKg = 60.0, currentKg = 60.0)

        assertEquals(1.0, progress)
    }

    @Test
    fun goalProgress_movedAwayFromGoal_isNegative() {
        val progress = goalProgress(baselineKg = 60.0, targetKg = 59.0, currentKg = 60.5)

        assertEquals(-0.5, progress)
    }

    @Test
    fun goalProgress_overachieved_isGreaterThanOne() {
        val progress = goalProgress(baselineKg = 60.0, targetKg = 59.0, currentKg = 58.0)

        assertEquals(2.0, progress)
    }

    @Test
    fun weightGoalProgress_usesBaselineAndTargetConstants() {
        val progress = weightGoalProgress(currentWeightKg = 59.5)

        assertEquals(0.5, progress)
    }

    @Test
    fun daysUntilWeightDeadline_onDeadline_isZero() {
        val days = daysUntilWeightDeadline(today = WEIGHT_DEADLINE)

        assertEquals(0L, days)
    }

    @Test
    fun daysUntilWeightDeadline_dayAfterDeadline_isMinusOne() {
        val days = daysUntilWeightDeadline(today = WEIGHT_DEADLINE.plusDays(1))

        assertEquals(-1L, days)
    }

    @Test
    fun isWeightGoalOverdue_afterDeadlineAndNotAchieved_isTrue() {
        val overdue =
            isWeightGoalOverdue(today = WEIGHT_DEADLINE.plusDays(1), progress = 0.9)

        assertTrue(overdue)
    }

    @Test
    fun isWeightGoalOverdue_afterDeadlineAndAchieved_isFalse() {
        val overdue =
            isWeightGoalOverdue(today = WEIGHT_DEADLINE.plusDays(1), progress = 1.0)

        assertFalse(overdue)
    }

    @Test
    fun isWeightGoalOverdue_beforeDeadline_isFalse() {
        val overdue =
            isWeightGoalOverdue(today = WEIGHT_DEADLINE.minusDays(1), progress = 0.0)

        assertFalse(overdue)
    }

    @Test
    fun isWeightGoalOverdue_onDeadlineAndNotAchieved_isFalse() {
        val overdue =
            isWeightGoalOverdue(today = WEIGHT_DEADLINE, progress = 0.9)

        assertFalse(overdue)
    }
}
