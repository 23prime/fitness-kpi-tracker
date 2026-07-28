package com.okkey.fitnesskpitracker.ui.entry

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.ManualField
import com.okkey.fitnesskpitracker.data.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
class EntryViewModelTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MetricsRepository
    private lateinit var viewModel: EntryViewModel
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
        viewModel = EntryViewModel(repository, initialDate = today)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_usesInitialDateAndEmptyFields() =
        runTest {
            dispatcher.scheduler.advanceUntilIdle()
            val state = viewModel.uiState.value

            assertEquals(today, state.date)
            assertEquals("", state.stepsInput)
            assertEquals("", state.cyclingDistanceInput)
            assertEquals("", state.weightInput)
            assertEquals("", state.workoutSetsInput)
            assertTrue(state.isSaveEnabled)
        }

    @Test
    fun onDateSelected_loadsExistingEffectiveValues() =
        runTest {
            val otherDate = LocalDate.of(2026, 7, 20)
            repository.saveManual(otherDate, steps = 4_000L, cyclingDistanceKm = 5.0, weightKg = 59.5, workoutSets = 21)
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onDateSelected(otherDate)
            dispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(otherDate, state.date)
            assertEquals("4000", state.stepsInput)
            assertEquals("5.0", state.cyclingDistanceInput)
            assertEquals("59.5", state.weightInput)
            assertEquals("21", state.workoutSetsInput)
        }

    @Test
    fun onReload_discardsUnsavedEditsAndReloadsPersistedValues() =
        runTest {
            repository.saveManual(today, steps = 4_000L, cyclingDistanceKm = null, weightKg = null, workoutSets = null)
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onFieldChanged(ManualField.STEPS, "9999")

            viewModel.onReload()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals("4000", viewModel.uiState.value.stepsInput)
        }

    @Test
    fun onFieldChanged_stepsNegativeValue_setsErrorAndDisablesSave() {
        viewModel.onFieldChanged(ManualField.STEPS, "-1")

        val state = viewModel.uiState.value
        assertEquals("-1", state.stepsInput)
        assertTrue(state.stepsError != null)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun onFieldChanged_stepsNonNumeric_setsErrorAndDisablesSave() {
        viewModel.onFieldChanged(ManualField.STEPS, "abc")

        val state = viewModel.uiState.value
        assertTrue(state.stepsError != null)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun onFieldChanged_stepsEmptyValue_isValidAndClearsError() {
        viewModel.onFieldChanged(ManualField.STEPS, "1")
        viewModel.onFieldChanged(ManualField.STEPS, "")

        val state = viewModel.uiState.value
        assertNull(state.stepsError)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun onFieldChanged_weightValidDecimal_isValid() {
        viewModel.onFieldChanged(ManualField.WEIGHT, "59.5")

        val state = viewModel.uiState.value
        assertNull(state.weightError)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun onFieldChanged_workoutSetsBeyondIntRange_setsErrorAndDisablesSave() {
        // Fits in a Long (validateLong would accept it) but overflows Int, which is
        // the type workoutSets is actually persisted as; the input must be rejected
        // rather than silently saved as null.
        viewModel.onFieldChanged(ManualField.WORKOUT_SETS, "99999999999")

        val state = viewModel.uiState.value
        assertTrue(state.workoutSetsError != null)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun onSave_persistsAllFieldsViaRepository() =
        runTest {
            viewModel.onFieldChanged(ManualField.STEPS, "4000")
            viewModel.onFieldChanged(ManualField.CYCLING_DISTANCE, "5.0")
            viewModel.onFieldChanged(ManualField.WEIGHT, "59.5")
            viewModel.onFieldChanged(ManualField.WORKOUT_SETS, "21")

            viewModel.onSave()
            dispatcher.scheduler.advanceUntilIdle()

            val saved = repository.findEffectiveByDate(today)
            assertEquals(4_000L, saved.steps)
            assertEquals(5.0, saved.cyclingDistanceKm)
            assertEquals(59.5, saved.weightKg)
            assertEquals(21, saved.workoutSets)
        }

    @Test
    fun onSave_emitsSaveCompletedEvent() =
        runTest {
            val events = mutableListOf<Unit>()
            val collectJob = launch(dispatcher) { viewModel.saveCompleted.toList(events) }

            viewModel.onFieldChanged(ManualField.STEPS, "4000")
            viewModel.onSave()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, events.size)
            collectJob.cancel()
        }

    @Test
    fun onClearField_steps_clearsInputLocallyWithoutTouchingRepository() =
        runTest {
            viewModel.onFieldChanged(ManualField.STEPS, "4000")
            viewModel.onFieldChanged(ManualField.CYCLING_DISTANCE, "5.0")

            viewModel.onClearField(ManualField.STEPS)

            val state = viewModel.uiState.value
            assertEquals("", state.stepsInput)
            assertEquals("5.0", state.cyclingDistanceInput)
            val persisted = repository.findEffectiveByDate(today)
            assertNull(persisted.steps)
            assertNull(persisted.cyclingDistanceKm)
        }

    @Test
    fun onClearField_thenSave_persistsClearedFieldAsNull() =
        runTest {
            viewModel.onFieldChanged(ManualField.STEPS, "4000")
            viewModel.onFieldChanged(ManualField.CYCLING_DISTANCE, "5.0")
            viewModel.onSave()
            dispatcher.scheduler.advanceUntilIdle()

            viewModel.onClearField(ManualField.STEPS)
            viewModel.onSave()
            dispatcher.scheduler.advanceUntilIdle()

            val saved = repository.findEffectiveByDate(today)
            assertNull(saved.steps)
            assertEquals(5.0, saved.cyclingDistanceKm)
        }
}
