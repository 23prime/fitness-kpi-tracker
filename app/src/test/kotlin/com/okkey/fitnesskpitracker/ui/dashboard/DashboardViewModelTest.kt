package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.FakeHealthConnectGateway
import com.okkey.fitnesskpitracker.data.HEALTH_CONNECT_PERMISSIONS
import com.okkey.fitnesskpitracker.data.HealthConnectAvailability
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.data.WeightPoint
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_START_DATE
import com.okkey.fitnesskpitracker.domain.daysUntilWeightDeadline
import com.okkey.fitnesskpitracker.domain.weightGoalProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
    fun initialState_noData_activityScoreZeroAndWeightHasNoRecord() =
        runTest {
            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(today, state.date)
            assertEquals(0.0, state.activityScore)
            assertEquals(0.0, state.activityAchievement)
            assertNull(state.currentWeightKg)
            assertNull(state.weightProgress)
            assertFalse(state.isWeightOverdue)
            assertEquals(daysUntilWeightDeadline(today), state.daysUntilDeadline)
            assertEquals(emptyList(), state.weightHistory)
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
    fun refresh_computesActivityScoreAndAchievementFromEffectiveValues() =
        runTest {
            repository.saveManual(today, steps = 5_000L, cyclingDistanceKm = 10.0, weightKg = null, workoutSets = 10)
            dispatcher.scheduler.advanceUntilIdle()

            val viewModel = DashboardViewModel(repository, gateway) { today }
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

    @Test
    fun onReload_reflectsDataSavedAfterInitialLoad() =
        runTest {
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.canGoToPreviousDay)
        }

    @Test
    fun onReload_reEvaluatesTodayOnEachRefreshSoUpperBoundTracksDateRollover() =
        runTest {
            var mutableToday = today
            val viewModel = DashboardViewModel(repository, gateway) { mutableToday }
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
            val viewModel = DashboardViewModel(repository, gateway) { today }
            controlledDispatcher.scheduler.advanceUntilIdle()
            viewModel.onPreviousDay()
            controlledDispatcher.scheduler.advanceUntilIdle()
            assertEquals(today.minusDays(1), viewModel.uiState.value.date)

            viewModel.onNextDay()
            viewModel.onNextDay()
            controlledDispatcher.scheduler.advanceUntilIdle()

            assertEquals(today, viewModel.uiState.value.date)
        }

    @Test
    fun healthConnectBanner_noneGranted_showsRequestPermission() =
        runTest {
            val noPermissionGateway = FakeHealthConnectGateway(grantedPermissions = emptySet())
            val viewModel = DashboardViewModel(repository, noPermissionGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(HealthConnectBannerState.REQUEST_PERMISSION, viewModel.uiState.value.healthConnectBannerState)
        }

    @Test
    fun healthConnectBanner_partiallyGranted_showsNone() =
        runTest {
            val partial = setOf(HEALTH_CONNECT_PERMISSIONS.first())
            val partialGateway = FakeHealthConnectGateway(grantedPermissions = partial)
            val viewModel = DashboardViewModel(repository, partialGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(HealthConnectBannerState.NONE, viewModel.uiState.value.healthConnectBannerState)
        }

    @Test
    fun healthConnectBanner_sdkUnavailable_showsUnavailable() =
        runTest {
            val unavailableGateway = FakeHealthConnectGateway(availability = HealthConnectAvailability.UNAVAILABLE)
            val viewModel = DashboardViewModel(repository, unavailableGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(HealthConnectBannerState.UNAVAILABLE, viewModel.uiState.value.healthConnectBannerState)
        }

    @Test
    fun onPermissionResult_noneGranted_emitsPermissionDeniedEvent() =
        runTest {
            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()
            val events = mutableListOf<Unit>()
            val collectJob = launch(dispatcher) { viewModel.permissionDeniedEvent.toList(events) }

            viewModel.onPermissionResult(emptySet())
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, events.size)
            collectJob.cancel()
        }

    @Test
    fun onPermissionResult_someGranted_doesNotEmitPermissionDeniedEvent() =
        runTest {
            val viewModel = DashboardViewModel(repository, gateway) { today }
            dispatcher.scheduler.advanceUntilIdle()
            val events = mutableListOf<Unit>()
            val collectJob = launch(dispatcher) { viewModel.permissionDeniedEvent.toList(events) }

            viewModel.onPermissionResult(setOf(HEALTH_CONNECT_PERMISSIONS.first()))
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, events.size)
            collectJob.cancel()
        }

    @Test
    fun onPermissionResult_refreshesBannerState() =
        runTest {
            val mutableGateway = FakeHealthConnectGateway(grantedPermissions = emptySet())
            val viewModel = DashboardViewModel(repository, mutableGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(HealthConnectBannerState.REQUEST_PERMISSION, viewModel.uiState.value.healthConnectBannerState)

            mutableGateway.grantedPermissions = HEALTH_CONNECT_PERMISSIONS
            viewModel.onPermissionResult(HEALTH_CONNECT_PERMISSIONS)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(HealthConnectBannerState.NONE, viewModel.uiState.value.healthConnectBannerState)
        }

    @Test
    fun onResume_syncsHealthConnectDataSilentlyWithoutLoadingOrSnackbar() =
        runTest {
            val syncGateway = FakeHealthConnectGateway(dailySteps = mapOf(today to 8_000L))
            val viewModel = DashboardViewModel(repository, syncGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()
            val events = mutableListOf<Unit>()
            val collectJob = launch(dispatcher) { viewModel.syncFailedEvent.toList(events) }

            viewModel.onResume()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(8_000L, viewModel.uiState.value.steps)
            assertFalse(viewModel.uiState.value.isSyncing)
            assertEquals(0, events.size)
            collectJob.cancel()
        }

    @Test
    fun onManualRefresh_success_syncsHealthConnectDataAndClearsLoading() =
        runTest {
            val syncGateway = FakeHealthConnectGateway(dailySteps = mapOf(today to 8_000L))
            val viewModel = DashboardViewModel(repository, syncGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onManualRefresh()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(8_000L, viewModel.uiState.value.steps)
            assertFalse(viewModel.uiState.value.isSyncing)
        }

    @Test
    fun onManualRefresh_readFailure_emitsSyncFailedEventAndClearsLoading() =
        runTest {
            val failingGateway = FakeHealthConnectGateway(readDailyStepsError = IllegalStateException("boom"))
            val viewModel = DashboardViewModel(repository, failingGateway) { today }
            dispatcher.scheduler.advanceUntilIdle()
            val events = mutableListOf<Unit>()
            val collectJob = launch(dispatcher) { viewModel.syncFailedEvent.toList(events) }

            viewModel.onManualRefresh()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, events.size)
            assertFalse(viewModel.uiState.value.isSyncing)
            collectJob.cancel()
        }

    @Test
    fun onManualRefresh_resumeCompletesWhileManualRefreshStillInFlight_keepsLoadingUntilManualRefreshFinishes() =
        runTest {
            val controlledDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(controlledDispatcher)
            val gate = CompletableDeferred<Unit>()
            val syncGateway =
                FakeHealthConnectGateway(dailySteps = mapOf(today to 8_000L)).apply {
                    onFirstReadDailySteps = { gate.await() }
                }
            val viewModel = DashboardViewModel(repository, syncGateway) { today }
            controlledDispatcher.scheduler.advanceUntilIdle()

            viewModel.onManualRefresh()
            controlledDispatcher.scheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isSyncing)

            viewModel.onResume()
            controlledDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSyncing)

            gate.complete(Unit)
            controlledDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSyncing)
        }

    @Test
    fun onManualRefresh_overlappingOnResume_stillClearsLoadingWhenManualRefreshFinishes() =
        runTest {
            val controlledDispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(controlledDispatcher)
            val syncGateway = FakeHealthConnectGateway(dailySteps = mapOf(today to 8_000L))
            val viewModel = DashboardViewModel(repository, syncGateway) { today }
            controlledDispatcher.scheduler.advanceUntilIdle()

            viewModel.onManualRefresh()
            controlledDispatcher.scheduler.runCurrent()
            viewModel.onResume()
            controlledDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSyncing)
        }
}
