package com.okkey.fitnesskpitracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val COLOR_PRIMARY = 0xFF9ECAFFL
private const val COLOR_ON_PRIMARY = 0xFF003258L
private const val COLOR_PRIMARY_CONTAINER = 0xFF00497DL
private const val COLOR_ON_PRIMARY_CONTAINER = 0xFFD1E4FFL
private const val COLOR_SECONDARY = 0xFFBBC7DBL
private const val COLOR_ON_SECONDARY = 0xFF253140L

internal val BlueDarkColorScheme =
    darkColorScheme(
        primary = Color(COLOR_PRIMARY),
        onPrimary = Color(COLOR_ON_PRIMARY),
        primaryContainer = Color(COLOR_PRIMARY_CONTAINER),
        onPrimaryContainer = Color(COLOR_ON_PRIMARY_CONTAINER),
        secondary = Color(COLOR_SECONDARY),
        onSecondary = Color(COLOR_ON_SECONDARY),
    )

@Composable
fun FitnessKpiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BlueDarkColorScheme, content = content)
}
