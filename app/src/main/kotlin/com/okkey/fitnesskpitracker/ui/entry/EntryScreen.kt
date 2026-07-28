package com.okkey.fitnesskpitracker.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.ManualField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = uiState.date.toString(), modifier = Modifier.weight(1f))
            TextButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.entry_button_select_date))
            }
        }

        EntryFields(
            uiState = uiState,
            onFieldChanged = viewModel::onFieldChanged,
            onClearField = viewModel::onClearField,
        )

        Button(
            onClick = { viewModel.onSave() },
            enabled = uiState.isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.entry_button_save))
        }
    }

    if (showDatePicker) {
        EntryDatePickerDialog(
            initialDate = uiState.date,
            onDateSelected = viewModel::onDateSelected,
            onDismissRequest = { showDatePicker = false },
        )
    }
}

@Composable
private fun EntryFields(
    uiState: EntryUiState,
    onFieldChanged: (ManualField, String) -> Unit,
    onClearField: (ManualField) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EntryField(
            label = stringResource(R.string.entry_label_steps),
            value = uiState.stepsInput,
            error = uiState.stepsError,
            onValueChange = { onFieldChanged(ManualField.STEPS, it) },
            onClear = { onClearField(ManualField.STEPS) },
        )
        EntryField(
            label = stringResource(R.string.entry_label_cycling_distance),
            value = uiState.cyclingDistanceInput,
            error = uiState.cyclingDistanceError,
            onValueChange = { onFieldChanged(ManualField.CYCLING_DISTANCE, it) },
            onClear = { onClearField(ManualField.CYCLING_DISTANCE) },
        )
        EntryField(
            label = stringResource(R.string.entry_label_weight),
            value = uiState.weightInput,
            error = uiState.weightError,
            onValueChange = { onFieldChanged(ManualField.WEIGHT, it) },
            onClear = { onClearField(ManualField.WEIGHT) },
        )
        EntryField(
            label = stringResource(R.string.entry_label_workout_sets),
            value = uiState.workoutSetsInput,
            error = uiState.workoutSetsError,
            onValueChange = { onFieldChanged(ManualField.WORKOUT_SETS, it) },
            onClear = { onClearField(ManualField.WORKOUT_SETS) },
        )
    }
}

@Composable
private fun EntryField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.entry_button_clear))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toUtcEpochMillis())
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirmDateSelection(datePickerState, onDateSelected)
                onDismissRequest()
            }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun onConfirmDateSelection(
    datePickerState: DatePickerState,
    onDateSelected: (LocalDate) -> Unit,
) {
    datePickerState.selectedDateMillis?.let { millis ->
        onDateSelected(millis.toLocalDateFromUtcEpochMillis())
    }
}

private fun LocalDate.toUtcEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateFromUtcEpochMillis(): LocalDate =
    Instant
        .ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
