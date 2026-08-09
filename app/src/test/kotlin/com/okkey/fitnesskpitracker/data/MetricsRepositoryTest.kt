package com.okkey.fitnesskpitracker.data

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MetricsRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = MetricsRepository(database.dailyMetricsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun findEffectiveByDate_noRecord_returnsAllNull() =
        runTest {
            val values = repository.findEffectiveByDate(LocalDate.of(2026, 7, 28))

            assertNull(values.steps)
            assertNull(values.cyclingDistanceKm)
            assertNull(values.weightKg)
            assertNull(values.workoutSets)
        }

    @Test
    fun findEffectiveByDate_manualOnly_returnsManualValues() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            repository.saveManual(date, steps = 4_000L, cyclingDistanceKm = 5.0, weightKg = 59.5, workoutSets = 21)

            val values = repository.findEffectiveByDate(date)

            assertEquals(4_000L, values.steps)
            assertEquals(5.0, values.cyclingDistanceKm)
            assertEquals(59.5, values.weightKg)
            assertEquals(21, values.workoutSets)
        }

    @Test
    fun findEffectiveByDate_manualOverridesHealthConnect() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            database.dailyMetricsDao().upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )
            repository.saveManual(date, steps = 4_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)

            val values = repository.findEffectiveByDate(date)

            assertEquals(4_000L, values.steps)
            assertNull(values.cyclingDistanceKm)
            assertEquals(60.0, values.weightKg)
        }

    @Test
    fun findSplitByDate_noRecord_returnsAllNullForBoth() =
        runTest {
            val (manual, healthConnect) = repository.findSplitByDate(LocalDate.of(2026, 7, 28))

            assertNull(manual.steps)
            assertNull(manual.cyclingDistanceKm)
            assertNull(manual.weightKg)
            assertNull(manual.workoutSets)
            assertNull(healthConnect.steps)
            assertNull(healthConnect.cyclingDistanceKm)
            assertNull(healthConnect.weightKg)
            assertNull(healthConnect.workoutSets)
        }

    @Test
    fun findSplitByDate_keepsManualAndHealthConnectSeparate() =
        runTest {
            val date = LocalDate.of(2026, 7, 28)
            database.dailyMetricsDao().upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(8_000L),
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )
            repository.saveManual(date, steps = 4_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = 21)

            val (manual, healthConnect) = repository.findSplitByDate(date)

            assertEquals(4_000L, manual.steps)
            assertNull(manual.cyclingDistanceKm)
            assertNull(manual.weightKg)
            assertEquals(21, manual.workoutSets)
            assertEquals(8_000L, healthConnect.steps)
            assertNull(healthConnect.cyclingDistanceKm)
            assertEquals(60.0, healthConnect.weightKg)
            assertNull(healthConnect.workoutSets)
        }

    @Test
    fun findLatestWeightKgOnOrBefore_noRecord_returnsNull() =
        runTest {
            val result = repository.findLatestWeightKgOnOrBefore(LocalDate.of(2026, 7, 28))

            assertNull(result)
        }

    @Test
    fun findLatestWeightKgOnOrBefore_fallsBackToMostRecentPriorRecord() =
        runTest {
            val recordedDate = LocalDate.of(2026, 7, 20)
            repository.saveManual(
                recordedDate,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 59.5,
                workoutSets = null,
            )

            val result = repository.findLatestWeightKgOnOrBefore(LocalDate.of(2026, 7, 28))

            assertEquals(59.5, result)
        }

    @Test
    fun findEarliestDate_noRecord_returnsNull() =
        runTest {
            val result = repository.findEarliestDate()

            assertNull(result)
        }

    @Test
    fun findEarliestDate_returnsOldestRecordedDate() =
        runTest {
            val oldest = LocalDate.of(2026, 7, 1)
            repository.saveManual(oldest, steps = null, cyclingDistanceKm = null, weightKg = null, workoutSets = null)
            repository.saveManual(
                LocalDate.of(2026, 7, 15),
                steps = 4_000L,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )

            val result = repository.findEarliestDate()

            assertEquals(oldest, result)
        }

    @Test
    fun findWeightRange_noRecords_returnsEmptyList() =
        runTest {
            val result = repository.findWeightRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30))

            assertEquals(emptyList(), result)
        }

    @Test
    fun findWeightRange_skipsDaysWithoutWeight() =
        runTest {
            repository.saveManual(
                LocalDate.of(2026, 8, 1),
                steps = 4_000L,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            repository.saveManual(
                LocalDate.of(2026, 8, 2),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 59.8,
                workoutSets = null,
            )

            val result = repository.findWeightRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30))

            assertEquals(listOf(WeightPoint(LocalDate.of(2026, 8, 2), 59.8)), result)
        }

    @Test
    fun findWeightRange_manualOverridesHealthConnect() =
        runTest {
            val date = LocalDate.of(2026, 8, 5)
            database.dailyMetricsDao().upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Skip,
                weightKg = HealthConnectFieldUpdate.Write(60.0),
            )
            repository.saveManual(date, steps = null, cyclingDistanceKm = null, weightKg = 59.5, workoutSets = null)

            val result = repository.findWeightRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30))

            assertEquals(listOf(WeightPoint(date, 59.5)), result)
        }

    @Test
    fun findWeightRange_excludesRecordsOutsideRange() =
        runTest {
            repository.saveManual(
                LocalDate.of(2026, 7, 31),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 60.5,
                workoutSets = null,
            )
            repository.saveManual(
                LocalDate.of(2026, 10, 1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 58.0,
                workoutSets = null,
            )

            val result = repository.findWeightRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30))

            assertEquals(emptyList(), result)
        }

    @Test
    fun findWeightRange_returnsPointsOrderedByDate() =
        runTest {
            repository.saveManual(
                LocalDate.of(2026, 8, 10),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 59.9,
                workoutSets = null,
            )
            repository.saveManual(
                LocalDate.of(2026, 8, 5),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 60.0,
                workoutSets = null,
            )

            val result = repository.findWeightRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30))

            assertEquals(
                listOf(
                    WeightPoint(LocalDate.of(2026, 8, 5), 60.0),
                    WeightPoint(LocalDate.of(2026, 8, 10), 59.9),
                ),
                result,
            )
        }

    @Test
    fun findActivityScoreRange_noRecords_returnsAllNullScores() =
        runTest {
            val result =
                repository.findActivityScoreRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))

            assertEquals(
                listOf(
                    DailyActivityScorePoint(LocalDate.of(2026, 8, 1), null),
                    DailyActivityScorePoint(LocalDate.of(2026, 8, 2), null),
                    DailyActivityScorePoint(LocalDate.of(2026, 8, 3), null),
                ),
                result,
            )
        }

    @Test
    fun findActivityScoreRange_recordedDay_returnsComputedScore() =
        runTest {
            val date = LocalDate.of(2026, 8, 2)
            repository.saveManual(date, steps = 4_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)

            val result = repository.findActivityScoreRange(date, date)

            assertEquals(listOf(DailyActivityScorePoint(date, 80.0)), result)
        }

    @Test
    fun findActivityScoreRange_rowWithOnlyWeight_treatsAsNoData() =
        runTest {
            val date = LocalDate.of(2026, 8, 2)
            repository.saveManual(date, steps = null, cyclingDistanceKm = null, weightKg = 59.5, workoutSets = null)

            val result = repository.findActivityScoreRange(date, date)

            assertEquals(listOf(DailyActivityScorePoint(date, null)), result)
        }

    @Test
    fun findActivityScoreRange_healthConnectZeroSteps_returnsZeroScoreNotNull() =
        runTest {
            val date = LocalDate.of(2026, 8, 2)
            database.dailyMetricsDao().upsertHealthConnect(
                date,
                steps = HealthConnectFieldUpdate.Write(0L),
                weightKg = HealthConnectFieldUpdate.Skip,
            )

            val result = repository.findActivityScoreRange(date, date)

            assertEquals(listOf(DailyActivityScorePoint(date, 0.0)), result)
        }

    @Test
    fun syncHealthConnect_partialPermission_onlyUpdatesGrantedField() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            val gateway =
                FakeHealthConnectGateway(
                    grantedPermissions = setOf(HealthPermission.getReadPermission(WeightRecord::class)),
                    weightSamples = listOf(WeightSample(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), 60.0)),
                )

            repository.syncHealthConnect(gateway, today)

            val healthConnect = repository.findSplitByDate(today).second
            assertNull(healthConnect.steps)
            assertEquals(60.0, healthConnect.weightKg)
        }

    @Test
    fun syncHealthConnect_doesNotOverwriteManualValues() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            repository.saveManual(today, steps = 1_000L, cyclingDistanceKm = null, weightKg = 58.0, workoutSets = null)
            val gateway =
                FakeHealthConnectGateway(
                    dailySteps = mapOf(today to 8_000L),
                    weightSamples = listOf(WeightSample(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), 60.0)),
                )

            repository.syncHealthConnect(gateway, today)

            val effective = repository.findEffectiveByDate(today)
            assertEquals(1_000L, effective.steps)
            assertEquals(58.0, effective.weightKg)
            val healthConnect = repository.findSplitByDate(today).second
            assertEquals(8_000L, healthConnect.steps)
            assertEquals(60.0, healthConnect.weightKg)
        }

    @Test
    fun syncHealthConnect_neverTouchesCyclingDistanceColumn() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            repository.saveManual(today, steps = null, cyclingDistanceKm = 5.0, weightKg = null, workoutSets = null)
            val gateway = FakeHealthConnectGateway(dailySteps = mapOf(today to 8_000L))

            repository.syncHealthConnect(gateway, today)

            assertEquals(5.0, repository.findEffectiveByDate(today).cyclingDistanceKm)
        }

    @Test
    fun syncHealthConnect_zeroRecordDay_savesZeroStepsAndNullWeight() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            val gateway = FakeHealthConnectGateway(dailySteps = emptyMap(), weightSamples = emptyList())

            repository.syncHealthConnect(gateway, today)

            val healthConnect = repository.findSplitByDate(today).second
            assertEquals(0L, healthConnect.steps)
            assertNull(healthConnect.weightKg)
        }

    @Test
    fun syncHealthConnect_readFailure_keepsExistingValuesAndReturnsFalse() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            database.dailyMetricsDao().upsertHealthConnect(
                today,
                steps = HealthConnectFieldUpdate.Write(5_000L),
                weightKg = HealthConnectFieldUpdate.Write(59.0),
            )
            val gateway = FakeHealthConnectGateway(readDailyStepsError = IllegalStateException("boom"))

            val succeeded = repository.syncHealthConnect(gateway, today)

            assertFalse(succeeded)
            val healthConnect = repository.findSplitByDate(today).second
            assertEquals(5_000L, healthConnect.steps)
        }

    @Test
    fun syncHealthConnect_noPermissionGranted_doesNothing() =
        runTest {
            val today = LocalDate.of(2026, 7, 28)
            val gateway = FakeHealthConnectGateway(grantedPermissions = emptySet())

            val succeeded = repository.syncHealthConnect(gateway, today)

            assertTrue(succeeded)
            assertNull(database.dailyMetricsDao().findByDate(today))
        }
}
