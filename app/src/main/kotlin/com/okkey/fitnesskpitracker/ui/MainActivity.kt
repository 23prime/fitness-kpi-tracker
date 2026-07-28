package com.okkey.fitnesskpitracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.okkey.fitnesskpitracker.FitnessKpiApplication
import com.okkey.fitnesskpitracker.ui.entry.EntryScreen
import com.okkey.fitnesskpitracker.ui.entry.EntryViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = application as FitnessKpiApplication
        val viewModelFactory = EntryViewModelFactory(application.metricsRepository)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    EntryScreen(viewModel(factory = viewModelFactory))
                }
            }
        }
    }
}
