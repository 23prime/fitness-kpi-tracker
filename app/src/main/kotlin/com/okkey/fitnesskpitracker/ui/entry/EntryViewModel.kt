package com.okkey.fitnesskpitracker.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okkey.fitnesskpitracker.data.ManualField
import com.okkey.fitnesskpitracker.data.MetricsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val ERROR_NOT_A_NUMBER = "数値を入力してください"
private const val ERROR_NEGATIVE = "0以上の値を入力してください"

data class EntryUiState(
    val date: LocalDate,
    val stepsInput: String = "",
    val cyclingDistanceInput: String = "",
    val weightInput: String = "",
    val workoutSetsInput: String = "",
    val stepsError: String? = null,
    val cyclingDistanceError: String? = null,
    val weightError: String? = null,
    val workoutSetsError: String? = null,
) {
    val isSaveEnabled: Boolean
        get() = stepsError == null && cyclingDistanceError == null && weightError == null && workoutSetsError == null
}

class EntryViewModel(
    private val repository: MetricsRepository,
    initialDate: LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EntryUiState(date = initialDate))
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _saveCompleted = MutableSharedFlow<Unit>()
    val saveCompleted: SharedFlow<Unit> = _saveCompleted.asSharedFlow()

    // Bumped on every load (date switch, field clear) and every user edit, so a
    // slow, out-of-order load completion never clobbers a newer load or an edit
    // the user made while that load was in flight.
    private var generation = 0

    init {
        loadDate(initialDate)
    }

    fun onDateSelected(date: LocalDate) {
        loadDate(date)
    }

    fun onFieldChanged(
        field: ManualField,
        text: String,
    ) {
        generation++
        _uiState.value =
            when (field) {
                ManualField.STEPS -> {
                    _uiState.value.copy(stepsInput = text, stepsError = validateLong(text))
                }

                ManualField.CYCLING_DISTANCE -> {
                    _uiState.value.copy(cyclingDistanceInput = text, cyclingDistanceError = validateDecimal(text))
                }

                ManualField.WEIGHT -> {
                    _uiState.value.copy(weightInput = text, weightError = validateDecimal(text))
                }

                ManualField.WORKOUT_SETS -> {
                    _uiState.value.copy(workoutSetsInput = text, workoutSetsError = validateInt(text))
                }
            }
    }

    fun onSave() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        viewModelScope.launch {
            repository.saveManual(
                date = state.date,
                steps = state.stepsInput.toLongOrNull(),
                cyclingDistanceKm = state.cyclingDistanceInput.toDoubleOrNull(),
                weightKg = state.weightInput.toDoubleOrNull(),
                workoutSets = state.workoutSetsInput.toIntOrNull(),
            )
            _saveCompleted.emit(Unit)
        }
    }

    fun onClearField(field: ManualField) {
        val date = _uiState.value.date
        val requestGeneration = ++generation
        viewModelScope.launch {
            repository.clearManualField(date, field)
            refresh(date, requestGeneration)
        }
    }

    private fun loadDate(date: LocalDate) {
        val requestGeneration = ++generation
        viewModelScope.launch {
            refresh(date, requestGeneration)
        }
    }

    private suspend fun refresh(
        date: LocalDate,
        requestGeneration: Int,
    ) {
        val values = repository.findEffectiveByDate(date)
        if (requestGeneration != generation) return
        _uiState.value =
            EntryUiState(
                date = date,
                stepsInput = values.steps?.toString() ?: "",
                cyclingDistanceInput = values.cyclingDistanceKm?.toString() ?: "",
                weightInput = values.weightKg?.toString() ?: "",
                workoutSetsInput = values.workoutSets?.toString() ?: "",
            )
    }

    private fun validateLong(text: String): String? {
        if (text.isEmpty()) return null
        val value = text.toLongOrNull()
        return when {
            value == null -> ERROR_NOT_A_NUMBER
            value < 0 -> ERROR_NEGATIVE
            else -> null
        }
    }

    private fun validateInt(text: String): String? {
        if (text.isEmpty()) return null
        val value = text.toIntOrNull()
        return when {
            value == null -> ERROR_NOT_A_NUMBER
            value < 0 -> ERROR_NEGATIVE
            else -> null
        }
    }

    private fun validateDecimal(text: String): String? {
        if (text.isEmpty()) return null
        val value = text.toDoubleOrNull()
        return when {
            value == null -> ERROR_NOT_A_NUMBER
            value < 0 -> ERROR_NEGATIVE
            else -> null
        }
    }
}
