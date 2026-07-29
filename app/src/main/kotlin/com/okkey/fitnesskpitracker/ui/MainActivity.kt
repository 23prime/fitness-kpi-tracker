package com.okkey.fitnesskpitracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.okkey.fitnesskpitracker.FitnessKpiApplication
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val application = application as FitnessKpiApplication
        val viewModelFactory = EntryViewModelFactory(application.metricsRepository)
        setContent {
            MaterialTheme(colorScheme = BlueDarkColorScheme) {
                Surface {
                    EntryScreen(viewModel(factory = viewModelFactory))
                }
            }
        }
    }
}
