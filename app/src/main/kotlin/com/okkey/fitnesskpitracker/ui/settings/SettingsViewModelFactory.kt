package com.okkey.fitnesskpitracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okkey.fitnesskpitracker.data.MetricsCsvRepository

class SettingsViewModelFactory(
    private val repository: MetricsCsvRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = SettingsViewModel(repository)
        return viewModel as T
    }
}
