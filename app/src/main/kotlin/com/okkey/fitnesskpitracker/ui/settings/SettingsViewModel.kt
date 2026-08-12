package com.okkey.fitnesskpitracker.ui.settings

import androidx.lifecycle.ViewModel
import com.okkey.fitnesskpitracker.data.CsvImportResult
import com.okkey.fitnesskpitracker.data.MetricsCsvRepository

class SettingsViewModel(
    private val repository: MetricsCsvRepository,
) : ViewModel() {
    suspend fun exportCsv(): String = repository.exportCsv()

    suspend fun importCsv(csv: String): CsvImportResult = repository.importCsv(csv)
}
