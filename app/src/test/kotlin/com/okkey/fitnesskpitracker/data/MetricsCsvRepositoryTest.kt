package com.okkey.fitnesskpitracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class MetricsCsvRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsCsvRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = MetricsCsvRepository(database.dailyMetricsCsvDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportCsv_noRows_returnsHeaderOnly() =
        runTest {
            val csv = repository.exportCsv()

            assertEquals(MetricsCsv.HEADER, csv)
        }

    @Test
    fun exportCsv_withRows_matchesMetricsCsvFormatOfAllRows() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            database.dailyMetricsDao().upsertManual(
                date,
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            val csv = repository.exportCsv()

            assertEquals(MetricsCsv.format(database.dailyMetricsCsvDao().findAll()), csv)
        }

    @Test
    fun importCsv_validCsv_replacesAllRowsAndReturnsSuccess() =
        runTest {
            database.dailyMetricsDao().upsertManual(
                LocalDate.of(2026, 7, 1),
                stepsManual = 1_000L,
                cyclingDistanceKmManual = null,
                weightKgManual = null,
                workoutSets = null,
            )
            val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,8000,,,,,,"

            val result = repository.importCsv(csv)

            assertIs<CsvImportResult.Success>(result)
            assertEquals(listOf(LocalDate.of(2026, 8, 1)), database.dailyMetricsCsvDao().findAll().map { it.date })
        }

    @Test
    fun importCsv_invalidCsv_returnsFailureWithoutModifyingExistingData() =
        runTest {
            database.dailyMetricsDao().upsertManual(
                LocalDate.of(2026, 7, 1),
                stepsManual = 1_000L,
                cyclingDistanceKmManual = null,
                weightKgManual = null,
                workoutSets = null,
            )
            val csv = MetricsCsv.HEADER + "\n" + "not-a-date,8000,,,,,,"

            val result = repository.importCsv(csv)

            assertIs<CsvImportResult.Failure>(result)
            assertIs<MetricsCsvParseResult.Failure.InvalidDate>(result.reason)
            assertEquals(listOf(LocalDate.of(2026, 7, 1)), database.dailyMetricsCsvDao().findAll().map { it.date })
        }
}
