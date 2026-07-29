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
import kotlin.test.assertNull

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
                stepsHealthConnect = 8_000L,
                cyclingDistanceKmHealthConnect = 10.0,
                weightKgHealthConnect = 60.0,
            )
            repository.saveManual(date, steps = 4_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)

            val values = repository.findEffectiveByDate(date)

            assertEquals(4_000L, values.steps)
            assertEquals(10.0, values.cyclingDistanceKm)
            assertEquals(60.0, values.weightKg)
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
}
