package com.okkey.fitnesskpitracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class DailyMetricsDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: DailyMetricsDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.dailyMetricsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertHealthConnect_insertsNewRowWithOnlyHealthConnectColumns() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(8_000L, row.stepsHealthConnect)
            assertEquals(60.0, row.weightKgHealthConnect)
            assertNull(row.cyclingDistanceKmHealthConnect)
            assertNull(row.stepsManual)
            assertNull(row.cyclingDistanceKmManual)
            assertNull(row.weightKgManual)
            assertNull(row.workoutSets)
        }

    @Test
    fun upsertHealthConnect_doesNotOverwriteExistingManualColumns() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertManual(
                date,
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(8_000L, row.stepsHealthConnect)
            assertEquals(60.0, row.weightKgHealthConnect)
            assertEquals(4_000L, row.stepsManual)
            assertEquals(5.0, row.cyclingDistanceKmManual)
            assertEquals(59.5, row.weightKgManual)
            assertEquals(21, row.workoutSets)
        }

    @Test
    fun upsertHealthConnect_neverTouchesCyclingDistanceColumn() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertManual(
                date,
                stepsManual = null,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = null,
                workoutSets = null,
            )

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(5.0, row.cyclingDistanceKmManual)
            assertNull(row.cyclingDistanceKmHealthConnect)
        }

    @Test
    fun upsertHealthConnect_skipLeavesExistingHealthConnectColumnUntouched() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Skip,
                weightKg = HealthConnectFieldUpdate.Write(59.0),
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(8_000L, row.stepsHealthConnect)
            assertEquals(59.0, row.weightKgHealthConnect)
        }

    @Test
    fun upsertHealthConnect_writeNullExplicitlyClearsExistingHealthConnectColumn() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(0L),
                weightKg = HealthConnectFieldUpdate.Write(null),
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(0L, row.stepsHealthConnect)
            assertNull(row.weightKgHealthConnect)
        }

    @Test
    fun upsertHealthConnect_allSkipDoesNotInsertRow() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Skip,
                weightKg = HealthConnectFieldUpdate.Skip,
            )

            val row = dao.findByDate(date)
            assertNull(row)
        }

    @Test
    fun upsertHealthConnect_skipAndWriteNullDoesNotInsertRow() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)

            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Skip,
                weightKg = HealthConnectFieldUpdate.Write(null),
            )

            val row = dao.findByDate(date)
            assertNull(row)
        }

    @Test
    fun upsertManual_insertsNewRowWithOnlyManualColumns() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)

            dao.upsertManual(
                date,
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(4_000L, row.stepsManual)
            assertEquals(5.0, row.cyclingDistanceKmManual)
            assertEquals(59.5, row.weightKgManual)
            assertEquals(21, row.workoutSets)
            assertNull(row.stepsHealthConnect)
            assertNull(row.cyclingDistanceKmHealthConnect)
            assertNull(row.weightKgHealthConnect)
        }

    @Test
    fun upsertManual_doesNotOverwriteExistingHealthConnectColumns() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )

            dao.upsertManual(
                date,
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertEquals(8_000L, row.stepsHealthConnect)
            assertEquals(60.0, row.weightKgHealthConnect)
            assertEquals(4_000L, row.stepsManual)
        }

    @Test
    fun upsertManual_canResetManualColumnToNull() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            dao.upsertManual(
                date,
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            dao.upsertManual(
                date,
                stepsManual = null,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            val row = dao.observeByDateRange(date, date).first().single()
            assertNull(row.stepsManual)
        }

    @Test
    fun observeByDateRange_emitsRowsWithinRangeOrderedByDate() =
        runTest {
            val day1 = LocalDate.of(2026, 7, 26)
            val day2 = LocalDate.of(2026, 7, 27)
            val day3 = LocalDate.of(2026, 7, 28)
            upsertStepsOnly(day3, steps = 3L)
            upsertStepsOnly(day1, steps = 1L)
            upsertStepsOnly(day2, steps = 2L)

            val rows = dao.observeByDateRange(day1, day2).first()

            assertEquals(listOf(day1, day2), rows.map { it.date })
        }

    @Test
    fun findLatestWithWeightOnOrBefore_noRows_returnsNull() =
        runTest {
            val result = dao.findLatestWithWeightOnOrBefore(LocalDate.of(2026, 7, 28))

            assertNull(result)
        }

    @Test
    fun findLatestWithWeightOnOrBefore_returnsMostRecentRowWithWeightOnOrBeforeDate() =
        runTest {
            val day1 = LocalDate.of(2026, 7, 20)
            val day2 = LocalDate.of(2026, 7, 25)
            val dayAfter = LocalDate.of(2026, 7, 29)
            dao.upsertManual(
                day1,
                stepsManual = null,
                cyclingDistanceKmManual = null,
                weightKgManual = 60.0,
                workoutSets = null,
            )
            dao.upsertManual(
                day2,
                stepsManual = null,
                cyclingDistanceKmManual = null,
                weightKgManual = 59.5,
                workoutSets = null,
            )
            dao.upsertManual(
                dayAfter,
                stepsManual = null,
                cyclingDistanceKmManual = null,
                weightKgManual = 59.0,
                workoutSets = null,
            )

            val result = dao.findLatestWithWeightOnOrBefore(LocalDate.of(2026, 7, 28))

            assertEquals(day2, result?.date)
            assertEquals(59.5, result?.weightKgManual)
        }

    @Test
    fun findLatestWithWeightOnOrBefore_skipsRowsWithoutWeight() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            upsertStepsOnly(date, steps = 4_000L)

            val result = dao.findLatestWithWeightOnOrBefore(date)

            assertNull(result)
        }

    @Test
    fun findEarliestDate_noRows_returnsNull() =
        runTest {
            val result = dao.findEarliestDate()

            assertNull(result)
        }

    @Test
    fun findEarliestDate_returnsOldestDateRegardlessOfWhichColumnsAreSet() =
        runTest {
            val oldest = LocalDate.of(2026, 7, 1)
            upsertStepsOnly(LocalDate.of(2026, 7, 20), steps = 4_000L)
            dao.upsertManual(
                oldest,
                stepsManual = null,
                cyclingDistanceKmManual = null,
                weightKgManual = 60.0,
                workoutSets = null,
            )

            val result = dao.findEarliestDate()

            assertEquals(oldest, result)
        }

    private suspend fun upsertStepsOnly(
        date: LocalDate,
        steps: Long,
    ) {
        dao.upsertManual(
            date,
            stepsManual = steps,
            cyclingDistanceKmManual = null,
            weightKgManual = null,
            workoutSets = null,
        )
    }
}
