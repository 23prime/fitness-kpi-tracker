package com.okkey.fitnesskpitracker.data

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MetricsCsvTest {
    @Test
    fun format_emptyList_returnsHeaderOnly() {
        val csv = MetricsCsv.format(emptyList())

        assertEquals(MetricsCsv.HEADER, csv)
    }

    @Test
    fun format_oneFullyPopulatedRow_writesAllColumns() {
        val entity =
            DailyMetricsEntity(
                date = LocalDate.parse("2026-08-01"),
                stepsHealthConnect = 8_000L,
                stepsManual = 8_500L,
                cyclingDistanceKmHealthConnect = 10.5,
                cyclingDistanceKmManual = 11.0,
                weightKgHealthConnect = 70.2,
                weightKgManual = 70.0,
                workoutSets = 5,
            )

        val csv = MetricsCsv.format(listOf(entity))

        assertEquals(
            MetricsCsv.HEADER + "\n" + "2026-08-01,8000,8500,10.5,11.0,70.2,70.0,5",
            csv,
        )
    }

    @Test
    fun format_rowWithNullFields_writesEmptyCells() {
        val entity =
            DailyMetricsEntity(
                date = LocalDate.parse("2026-08-02"),
                stepsHealthConnect = null,
                stepsManual = null,
                cyclingDistanceKmHealthConnect = null,
                cyclingDistanceKmManual = null,
                weightKgHealthConnect = null,
                weightKgManual = null,
                workoutSets = null,
            )

        val csv = MetricsCsv.format(listOf(entity))

        assertEquals(
            MetricsCsv.HEADER + "\n" + "2026-08-02,,,,,,,",
            csv,
        )
    }

    @Test
    fun parse_formattedOutput_roundTripsToSameEntities() {
        val entities =
            listOf(
                DailyMetricsEntity(
                    date = LocalDate.parse("2026-08-01"),
                    stepsHealthConnect = 8_000L,
                    stepsManual = null,
                    cyclingDistanceKmHealthConnect = null,
                    cyclingDistanceKmManual = 11.0,
                    weightKgHealthConnect = 70.2,
                    weightKgManual = null,
                    workoutSets = 5,
                ),
            )

        val result = MetricsCsv.parse(MetricsCsv.format(entities))

        assertIs<MetricsCsvParseResult.Success>(result)
        assertEquals(entities, result.entities)
    }

    @Test
    fun parse_headerMissingAColumn_returnsHeaderMismatch() {
        val csv = "date,steps_health_connect,steps_manual\n2026-08-01,8000,8500"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.HeaderMismatch>(result)
    }

    @Test
    fun parse_headerWithExtraColumn_returnsHeaderMismatch() {
        val csv = MetricsCsv.HEADER + ",extra\n"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.HeaderMismatch>(result)
    }

    @Test
    fun parse_headerColumnsReordered_resolvesByNameNotOrder() {
        val csv =
            "steps_manual,date,steps_health_connect,cycling_distance_km_manual," +
                "cycling_distance_km_health_connect,weight_kg_manual,weight_kg_health_connect,workout_sets\n" +
                "8500,2026-08-01,8000,11.0,,,70.2,5"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Success>(result)
        assertEquals(
            DailyMetricsEntity(
                date = LocalDate.parse("2026-08-01"),
                stepsHealthConnect = 8_000L,
                stepsManual = 8_500L,
                cyclingDistanceKmHealthConnect = null,
                cyclingDistanceKmManual = 11.0,
                weightKgHealthConnect = 70.2,
                weightKgManual = null,
                workoutSets = 5,
            ),
            result.entities.single(),
        )
    }

    @Test
    fun parse_unparsableDate_returnsInvalidDateWithLineNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,8000,,,,,,\n" + "not-a-date,8000,,,,,,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.InvalidDate>(result)
        assertEquals(3, result.lineNumber)
    }

    @Test
    fun parse_duplicateDate_returnsDuplicateDateWithLineNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,8000,,,,,,\n" + "2026-08-01,8500,,,,,,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.DuplicateDate>(result)
        assertEquals(3, result.lineNumber)
    }

    @Test
    fun parse_unparsableNumber_returnsInvalidNumberWithLineNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,not-a-number,,,,,,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.InvalidNumber>(result)
        assertEquals(2, result.lineNumber)
    }

    @Test
    fun parse_negativeNumber_isAccepted() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,,-5,,,,-70.0,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Success>(result)
        assertEquals(-5L, result.entities.single().stepsManual)
        assertEquals(-70.0, result.entities.single().weightKgManual)
    }

    @Test
    fun parse_rowWithFewerCellsThanHeader_returnsInvalidNumberWithLineNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,8000"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.InvalidNumber>(result)
        assertEquals(2, result.lineNumber)
    }

    @Test
    fun parse_blankLineBeforeInvalidRow_reportsCorrectPhysicalLineNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "\n" + "not-a-date,,,,,,,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.InvalidDate>(result)
        assertEquals(3, result.lineNumber)
    }

    @Test
    fun parse_crlfLineEndings_parsesSuccessfully() {
        val csv = MetricsCsv.HEADER + "\r\n" + "2026-08-01,8000,,,,,,\r\n"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Success>(result)
        assertEquals(8_000L, result.entities.single().stepsHealthConnect)
    }

    @Test
    fun parse_nonFiniteNumber_returnsInvalidNumber() {
        val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,,,,,,NaN,"

        val result = MetricsCsv.parse(csv)

        assertIs<MetricsCsvParseResult.Failure.InvalidNumber>(result)
    }
}
