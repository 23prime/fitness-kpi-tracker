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

@RunWith(RobolectricTestRunner::class)
class DailyMetricsCsvDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: DailyMetricsCsvDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.dailyMetricsCsvDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun findAll_noRows_returnsEmptyList() =
        runTest {
            val result = dao.findAll()

            assertEquals(emptyList(), result)
        }

    @Test
    fun findAll_returnsAllRowsOrderedByDate() =
        runTest {
            upsertStepsOnly(LocalDate.of(2026, 7, 20), steps = 4_000L)
            upsertStepsOnly(LocalDate.of(2026, 7, 10), steps = 3_000L)

            val result = dao.findAll()

            assertEquals(listOf(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20)), result.map { it.date })
        }

    @Test
    fun replaceAll_existingRows_areDeletedAndReplacedWithGivenEntities() =
        runTest {
            upsertStepsOnly(LocalDate.of(2026, 7, 10), steps = 3_000L)
            val replacement =
                DailyMetricsEntity(
                    date = LocalDate.of(2026, 8, 1),
                    stepsHealthConnect = 8_000L,
                    stepsManual = null,
                    cyclingDistanceKmHealthConnect = null,
                    cyclingDistanceKmManual = null,
                    weightKgHealthConnect = null,
                    weightKgManual = null,
                    workoutSets = null,
                )

            dao.replaceAll(listOf(replacement))

            assertEquals(listOf(replacement), dao.findAll())
        }

    @Test
    fun replaceAll_emptyList_deletesAllExistingRows() =
        runTest {
            upsertStepsOnly(LocalDate.of(2026, 7, 10), steps = 3_000L)

            dao.replaceAll(emptyList())

            assertEquals(emptyList(), dao.findAll())
        }

    private suspend fun upsertStepsOnly(
        date: LocalDate,
        steps: Long,
    ) {
        database.dailyMetricsDao().upsertManual(
            date,
            stepsManual = steps,
            cyclingDistanceKmManual = null,
            weightKgManual = null,
            workoutSets = null,
        )
    }
}
