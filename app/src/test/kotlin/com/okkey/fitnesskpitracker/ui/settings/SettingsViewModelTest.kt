package com.okkey.fitnesskpitracker.ui.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.MetricsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = MetricsRepository(database.dailyMetricsDao())
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportCsv_delegatesToRepository() =
        runTest {
            repository.saveManual(
                LocalDate.of(2026, 7, 28),
                steps = 4_000L,
                cyclingDistanceKm = 5.0,
                weightKg = 59.5,
                workoutSets = 21,
            )

            val csv = viewModel.exportCsv()

            assertEquals(repository.exportCsv(), csv)
        }
}
