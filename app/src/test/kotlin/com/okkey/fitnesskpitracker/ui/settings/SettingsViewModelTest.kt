package com.okkey.fitnesskpitracker.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.CsvImportResult
import com.okkey.fitnesskpitracker.data.MetricsCsv
import com.okkey.fitnesskpitracker.data.MetricsCsvRepository
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
class SettingsViewModelTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsCsvRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = MetricsCsvRepository(database.dailyMetricsCsvDao())
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportCsv_delegatesToRepository() =
        runTest {
            database.dailyMetricsDao().upsertManual(
                LocalDate.of(2026, 7, 28),
                stepsManual = 4_000L,
                cyclingDistanceKmManual = 5.0,
                weightKgManual = 59.5,
                workoutSets = 21,
            )

            val csv = viewModel.exportCsv()

            assertEquals(repository.exportCsv(), csv)
        }

    @Test
    fun importCsv_delegatesToRepository() =
        runTest {
            val csv = MetricsCsv.HEADER + "\n" + "2026-08-01,8000,,,,,,"

            val result = viewModel.importCsv(csv)

            assertIs<CsvImportResult.Success>(result)
            assertEquals(listOf(LocalDate.of(2026, 8, 1)), database.dailyMetricsCsvDao().findAll().map { it.date })
        }
}
