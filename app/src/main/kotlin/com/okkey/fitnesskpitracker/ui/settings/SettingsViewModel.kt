package com.okkey.fitnesskpitracker.ui.settings

import androidx.lifecycle.ViewModel
import com.okkey.fitnesskpitracker.data.MetricsRepository

class SettingsViewModel(
    private val repository: MetricsRepository,
) : ViewModel() {
    suspend fun exportCsv(): String = repository.exportCsv()
}
