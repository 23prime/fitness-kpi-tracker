package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okkey.fitnesskpitracker.data.MetricsRepository
import java.time.LocalDate

class DashboardViewModelFactory(
    private val repository: MetricsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = DashboardViewModel(repository, today = LocalDate::now)
        return viewModel as T
    }
}
