package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.domain.daysUntilWeightDeadline
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DashboardViewModelTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsRepository
    private val today = LocalDate.of(2026, 7, 28)
    private val dispatcher = UnconfinedTestDispatcher()

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
    fun initialState_noData_activityScoreZeroAndWeightHasNoRecord() =
        runTest {
            val viewModel = DashboardViewModel(repository, today)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(today, state.date)
            assertEquals(0.0, state.activityScore)
            assertEquals(0.0, state.activityAchievement)
            assertNull(state.currentWeightKg)
            assertNull(state.weightProgress)
            assertFalse(state.isWeightOverdue)
            assertEquals(daysUntilWeightDeadline(today), state.daysUntilDeadline)
        }

    @Test
    fun refresh_computesActivityScoreAndAchievementFromEffectiveValues() =
        runTest {
            repository.saveManual(today, steps = 5_000L, cyclingDistanceKm = 10.0, weightKg = null, workoutSets = 10)
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, today)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(225.0, state.activityScore)
            assertEquals(1.5, state.activityAchievement)
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

            val viewModel = DashboardViewModel(repository, today)
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

            val viewModel = DashboardViewModel(repository, afterDeadline)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isWeightOverdue)
        }

    @Test
    fun onReload_reflectsDataSavedAfterInitialLoad() =
        runTest {
            val viewModel = DashboardViewModel(repository, today)
            dispatcher.scheduler.advanceUntilIdle()

            repository.saveManual(today, steps = 5_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)
            viewModel.onReload()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(100.0, viewModel.uiState.value.activityScore)
        }
}
