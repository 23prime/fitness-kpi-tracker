package com.okkey.fitnesskpitracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.okkey.fitnesskpitracker.R
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val EXPORT_FILE_NAME_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
private const val EXPORT_MIME_TYPE = "text/csv"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportSuccessMessage = stringResource(R.string.settings_export_success)
    val exportFailedMessage = stringResource(R.string.settings_export_failed)

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val message =
                    try {
                        val csv = viewModel.exportCsv()
                        val outputStream =
                            context.contentResolver.openOutputStream(uri) ?: throw IOException("null output stream")
                        outputStream.use { it.write(csv.toByteArray()) }
                        exportSuccessMessage
                    } catch (_: IOException) {
                        exportFailedMessage
                    } catch (_: SecurityException) {
                        exportFailedMessage
                    }
                snackbarHostState.showSnackbar(message)
            }
        }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Button(
                onClick = { exportLauncher.launch(defaultExportFileName()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_button_export))
            }
        }
    }
}

private fun defaultExportFileName(): String {
    val date = LocalDate.now().format(EXPORT_FILE_NAME_DATE_FORMATTER)
    return "fitness-kpi-tracker-$date.csv"
}
