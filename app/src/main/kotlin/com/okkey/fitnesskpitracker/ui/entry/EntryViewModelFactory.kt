package com.okkey.fitnesskpitracker.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okkey.fitnesskpitracker.data.MetricsRepository
import java.time.LocalDate

class EntryViewModelFactory(
    private val repository: MetricsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = EntryViewModel(repository, initialDate = LocalDate.now())
        return viewModel as T
    }
}
