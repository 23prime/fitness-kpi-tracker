package com.okkey.fitnesskpitracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.okkey.fitnesskpitracker.FitnessKpiApplication
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.ui.dashboard.DashboardScreen
import com.okkey.fitnesskpitracker.ui.dashboard.DashboardViewModelFactory
import com.okkey.fitnesskpitracker.ui.entry.EntryScreen
import com.okkey.fitnesskpitracker.ui.entry.EntryViewModelFactory
import android.graphics.Color as AndroidColor

private const val COLOR_PRIMARY = 0xFF9ECAFFL
private const val COLOR_ON_PRIMARY = 0xFF003258L
private const val COLOR_PRIMARY_CONTAINER = 0xFF00497DL
private const val COLOR_ON_PRIMARY_CONTAINER = 0xFFD1E4FFL
private const val COLOR_SECONDARY = 0xFFBBC7DBL
private const val COLOR_ON_SECONDARY = 0xFF253140L

private val BlueDarkColorScheme =
    darkColorScheme(
        primary = Color(COLOR_PRIMARY),
        onPrimary = Color(COLOR_ON_PRIMARY),
        primaryContainer = Color(COLOR_PRIMARY_CONTAINER),
        onPrimaryContainer = Color(COLOR_ON_PRIMARY_CONTAINER),
        secondary = Color(COLOR_SECONDARY),
        onSecondary = Color(COLOR_ON_SECONDARY),
    )

private enum class AppScreen { DASHBOARD, ENTRY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val application = application as FitnessKpiApplication
        val repository = application.metricsRepository
        setContent {
            MaterialTheme(colorScheme = BlueDarkColorScheme) {
                Surface {
                    FitnessKpiApp(repository)
                }
            }
        }
    }
}

@Composable
private fun FitnessKpiApp(repository: MetricsRepository) {
    var selectedScreen by rememberSaveable { mutableStateOf(AppScreen.DASHBOARD) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel(factory = DashboardViewModelFactory(repository)))
                AppScreen.ENTRY -> EntryScreen(viewModel(factory = EntryViewModelFactory(repository)))
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = selectedScreen == AppScreen.DASHBOARD,
                onClick = { selectedScreen = AppScreen.DASHBOARD },
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_dashboard)) },
            )
            NavigationBarItem(
                selected = selectedScreen == AppScreen.ENTRY,
                onClick = { selectedScreen = AppScreen.ENTRY },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_entry)) },
            )
        }
    }
}
