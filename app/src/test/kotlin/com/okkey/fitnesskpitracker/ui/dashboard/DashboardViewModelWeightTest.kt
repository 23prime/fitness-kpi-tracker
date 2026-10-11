package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.FakeHealthConnectGateway
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.data.WeightPoint
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_START_DATE
import com.okkey.fitnesskpitracker.domain.weightGoalProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelWeightTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsRepository
    private val today = LocalDate.of(2026, 7, 28)
    private val dispatcher = UnconfinedTestDispatcher()
    private val gateway = FakeHealthConnectGateway()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val synchronousExecutor = Executor { it.run() }
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .setQueryExecutor(synchronousExecutor)
                .setTransactionExecutor(synchronousExecutor)
                .build()
        repository = MetricsRepository(database.dailyMetricsDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_weightHistory_containsOnlyPointsWithinStartDateAndDeadline() =
        runTest {
            repository.saveManual(
                WEIGHT_START_DATE.minusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 61.0,
                workoutSets = null,
            )
            repository.saveManual(
                WEIGHT_START_DATE,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 60.0,
                workoutSets = null,
            )
            repository.saveManual(
                WEIGHT_DEADLINE.plusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 58.0,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(WeightPoint(WEIGHT_START_DATE, 60.0)), state.weightHistory)
        }

    @Test
    fun refresh_afterDeadline_weightChartExtendsToToday() =
        runTest {
            val afterDeadline = WEIGHT_DEADLINE.plusDays(2)
            repository.saveManual(
                WEIGHT_DEADLINE.plusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 58.0,
                workoutSets = null,
            )
            repository.saveManual(
                afterDeadline.plusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 57.0,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, gateway) { afterDeadline }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(afterDeadline, state.weightChartEndDate)
            assertEquals(listOf(WeightPoint(WEIGHT_DEADLINE.plusDays(1), 58.0)), state.weightHistory)
        }

    @Test
    fun refresh_currentWeight_fallsBackToMostRecentPriorRecord() =
        runTest {
            val recordedDate = LocalDate.of(2026, 7, 20)
            repository.saveManual(
                recordedDate,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 59.5,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(59.5, state.currentWeightKg)
            assertEquals(weightGoalProgress(59.5), state.weightProgress)
        }

    @Test
    fun refresh_pastDeadlineWithUnmetGoal_marksWeightOverdue() =
        runTest {
            val afterDeadline = LocalDate.of(2026, 10, 1)
            repository.saveManual(
                afterDeadline,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = 59.5,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, gateway) { afterDeadline }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isWeightOverdue)
        }
}
