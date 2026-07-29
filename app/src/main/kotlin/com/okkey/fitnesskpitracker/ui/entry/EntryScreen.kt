package com.okkey.fitnesskpitracker.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.ManualField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val DATE_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

private const val COLOR_UNFOCUSED_LABEL = 0xFF9E9E9EL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMessage = stringResource(R.string.entry_save_success)

    LaunchedEffect(viewModel) {
        viewModel.saveCompleted.collect {
            snackbarHostState.showSnackbar(saveSuccessMessage)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.entry_title)) },
                actions = {
                    IconButton(onClick = viewModel::onReload) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.entry_button_reload))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        EntryContent(
            uiState = uiState,
            actions =
                EntryActions(
                    onSelectDateRequested = { showDatePicker = true },
                    onFieldChanged = viewModel::onFieldChanged,
                    onClearField = viewModel::onClearField,
                    onSave = viewModel::onSave,
                    onReload = viewModel::onReload,
                ),
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showDatePicker) {
        EntryDatePickerDialog(
            initialDate = uiState.date,
            onDateSelected = viewModel::onDateSelected,
            onDismissRequest = { showDatePicker = false },
        )
    }
}

private data class EntryActions(
    val onSelectDateRequested: () -> Unit,
    val onFieldChanged: (ManualField, String) -> Unit,
    val onClearField: (ManualField) -> Unit,
    val onSave: () -> Unit,
    val onReload: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryContent(
    uiState: EntryUiState,
    actions: EntryActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = actions.onReload,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = uiState.date.format(DATE_DISPLAY_FORMATTER), modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onSelectDateRequested) {
                    Text(stringResource(R.string.entry_button_select_date))
                }
            }

            EntryFields(
                uiState = uiState,
                onFieldChanged = actions.onFieldChanged,
                onClearField = actions.onClearField,
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    actions.onSave()
                },
                enabled = uiState.isSaveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.entry_button_save))
            }
        }
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
            colors = OutlinedTextFieldDefaults.colors(unfocusedLabelColor = Color(COLOR_UNFOCUSED_LABEL)),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.entry_button_clear))
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
