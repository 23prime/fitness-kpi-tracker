package com.okkey.fitnesskpitracker.data

import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals

class HealthConnectAggregationTest {
    private val zone: ZoneId = ZoneOffset.UTC

    @Test
    fun latestWeightPerDate_singleSample_returnsItsValue() {
        val date = LocalDate.of(2026, 7, 28)
        val samples = listOf(WeightSample(date.atStartOfDay(zone).toInstant(), 60.0))

        val result = latestWeightPerDate(samples, zone)

        assertEquals(mapOf(date to 60.0), result)
    }

    @Test
    fun latestWeightPerDate_multipleSamplesSameDay_returnsLastByInstant() {
        val date = LocalDate.of(2026, 7, 28)
        val morning = WeightSample(date.atTime(7, 0).atZone(zone).toInstant(), 60.5)
        val evening = WeightSample(date.atTime(21, 0).atZone(zone).toInstant(), 59.8)

        val result = latestWeightPerDate(listOf(evening, morning), zone)

        assertEquals(mapOf(date to 59.8), result)
    }

    @Test
    fun latestWeightPerDate_samplesAcrossDays_groupsIndependently() {
        val day1 = LocalDate.of(2026, 7, 27)
        val day2 = LocalDate.of(2026, 7, 28)
        val samples =
            listOf(
                WeightSample(day1.atStartOfDay(zone).toInstant(), 61.0),
                WeightSample(day2.atStartOfDay(zone).toInstant(), 60.0),
            )

        val result = latestWeightPerDate(samples, zone)

        assertEquals(mapOf(day1 to 61.0, day2 to 60.0), result)
    }

    @Test
    fun latestWeightPerDate_noSamples_returnsEmptyMap() {
        val result = latestWeightPerDate(emptyList(), zone)

        assertEquals(emptyMap(), result)
    }
}
