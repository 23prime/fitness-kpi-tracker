package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.domain.daysUntilWeightDeadline
import com.okkey.fitnesskpitracker.domain.weightGoalProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
            val viewModel = DashboardViewModel(repository) { today }
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

            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(5_000L, state.steps)
            assertEquals(10.0, state.cyclingDistanceKm)
            assertEquals(10, state.workoutSets)
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

            val viewModel = DashboardViewModel(repository) { today }
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

            val viewModel = DashboardViewModel(repository) { afterDeadline }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isWeightOverdue)
        }

    @Test
    fun onReload_reflectsDataSavedAfterInitialLoad() =
        runTest {
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()

            repository.saveManual(today, steps = 5_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)
            viewModel.onReload()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(100.0, viewModel.uiState.value.activityScore)
        }

    @Test
    fun onReload_reflectsSelectedDateNotToday() =
        runTest {
            val yesterday = today.minusDays(1)
            repository.saveManual(
                yesterday,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onPreviousDay()
            dispatcher.scheduler.advanceUntilIdle()

            repository.saveManual(
                yesterday,
                steps = 5_000L,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            viewModel.onReload()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(yesterday, state.date)
            assertEquals(100.0, state.activityScore)
        }

    @Test
    fun onPreviousDay_movesActivitySelectionButKeepsWeightSectionOnToday() =
        runTest {
            repository.saveManual(
                today.minusDays(5),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            repository.saveManual(today, steps = 5_000L, cyclingDistanceKm = null, weightKg = 59.5, workoutSets = null)
            dispatcher.scheduler.advanceUntilIdle()
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onPreviousDay()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(today.minusDays(1), state.date)
            assertEquals(0.0, state.activityScore)
            assertEquals(59.5, state.currentWeightKg)
            assertEquals(daysUntilWeightDeadline(today), state.daysUntilDeadline)
        }

    @Test
    fun onNextDay_movesSelectionForwardTowardToday() =
        runTest {
            repository.saveManual(
                today.minusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onPreviousDay()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(today.minusDays(1), viewModel.uiState.value.date)

            viewModel.onNextDay()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(today, viewModel.uiState.value.date)
        }

    @Test
    fun onNextDay_atToday_doesNothing() =
        runTest {
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onNextDay()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(today, state.date)
            assertFalse(state.canGoToNextDay)
        }

    @Test
    fun onPreviousDay_atEarliestRecordedDate_doesNothing() =
        runTest {
            val earliestDate = today.minusDays(1)
            repository.saveManual(
                earliestDate,
                steps = null,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            dispatcher.scheduler.advanceUntilIdle()
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onPreviousDay()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onPreviousDay()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(earliestDate, state.date)
            assertFalse(state.canGoToPreviousDay)
        }

    @Test
    fun onPreviousDay_noRecordedData_disablesPreviousDayImmediately() =
        runTest {
            val viewModel = DashboardViewModel(repository) { today }
            dispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canGoToPreviousDay)
        }

    @Test
    fun onReload_reEvaluatesTodayOnEachRefreshSoUpperBoundTracksDateRollover() =
        runTest {
            var mutableToday = today
            val viewModel = DashboardViewModel(repository) { mutableToday }
            dispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canGoToNextDay)

            mutableToday = today.plusDays(1)
            viewModel.onReload()
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.canGoToNextDay)

            viewModel.onNextDay()
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(mutableToday, state.date)
            assertFalse(state.canGoToNextDay)
        }

    @Test
    fun onNextDay_calledAgainBeforePriorRefreshCompletes_isRejectedNotDoubleAdvanced() =
        runTest {
            val controlledDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(controlledDispatcher)
            repository.saveManual(
                today.minusDays(1),
                steps = null,
                cyclingDistanceKm = null,
                weightKg = null,
                workoutSets = null,
            )
            val viewModel = DashboardViewModel(repository) { today }
            controlledDispatcher.scheduler.advanceUntilIdle()
            viewModel.onPreviousDay()
            controlledDispatcher.scheduler.advanceUntilIdle()
            assertEquals(today.minusDays(1), viewModel.uiState.value.date)

            viewModel.onNextDay()
            viewModel.onNextDay()
            controlledDispatcher.scheduler.advanceUntilIdle()

            assertEquals(today, viewModel.uiState.value.date)
        }
}
