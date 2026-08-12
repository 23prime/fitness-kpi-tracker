package com.okkey.fitnesskpitracker.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.CsvImportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val EXPORT_FILE_NAME_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
private const val EXPORT_MIME_TYPE = "text/csv"
private const val IMPORT_MIME_TYPE = "*/*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val messages = settingsMessages()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val message = exportCsvTo(context, uri, viewModel, messages)
                snackbarHostState.showSnackbar(message)
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingImportUri = uri
        }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        SettingsButtons(
            modifier = Modifier.padding(innerPadding),
            onExportClick = { exportLauncher.launch(defaultExportFileName()) },
            onImportClick = { importLauncher.launch(arrayOf(IMPORT_MIME_TYPE)) },
        )
    }

    val importUri = pendingImportUri
    if (importUri != null) {
        ImportConfirmDialog(
            onConfirm = {
                pendingImportUri = null
                scope.launch {
                    val message = importCsvFrom(context, importUri, viewModel, messages)
                    snackbarHostState.showSnackbar(message)
                }
            },
            onDismissRequest = { pendingImportUri = null },
        )
    }
}

@Composable
private fun SettingsButtons(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Button(onClick = onExportClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_button_export))
        }
        Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_button_import))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportConfirmDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.settings_import_confirm_title)) },
        text = { Text(stringResource(R.string.settings_import_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_import_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private data class SettingsMessages(
    val exportSuccess: String,
    val exportFailed: String,
    val importSuccess: String,
    val importFailed: String,
)

@Composable
private fun settingsMessages(): SettingsMessages =
    SettingsMessages(
        exportSuccess = stringResource(R.string.settings_export_success),
        exportFailed = stringResource(R.string.settings_export_failed),
        importSuccess = stringResource(R.string.settings_import_success),
        importFailed = stringResource(R.string.settings_import_failed),
    )

private suspend fun exportCsvTo(
    context: Context,
    uri: Uri,
    viewModel: SettingsViewModel,
    messages: SettingsMessages,
): String =
    try {
        val csv = viewModel.exportCsv()
        val outputStream = context.contentResolver.openOutputStream(uri) ?: throw IOException("null output stream")
        outputStream.use { it.write(csv.toByteArray()) }
        messages.exportSuccess
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        messages.exportFailed
    }

private suspend fun importCsvFrom(
    context: Context,
    uri: Uri,
    viewModel: SettingsViewModel,
    messages: SettingsMessages,
): String =
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw IOException("null input stream")
        val csv = inputStream.use { it.bufferedReader().readText() }
        when (viewModel.importCsv(csv)) {
            is CsvImportResult.Success -> messages.importSuccess
            is CsvImportResult.Failure -> messages.importFailed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        messages.importFailed
    }

private fun defaultExportFileName(): String {
    val date = LocalDate.now().format(EXPORT_FILE_NAME_DATE_FORMATTER)
    return "fitness-kpi-tracker-$date.csv"
}
