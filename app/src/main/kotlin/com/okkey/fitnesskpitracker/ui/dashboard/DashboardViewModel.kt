package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.domain.activityScore
import com.okkey.fitnesskpitracker.domain.dailyScoreAchievement
import com.okkey.fitnesskpitracker.domain.daysUntilWeightDeadline
import com.okkey.fitnesskpitracker.domain.isWeightGoalOverdue
import com.okkey.fitnesskpitracker.domain.weightGoalProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val date: LocalDate,
    val steps: Long? = null,
    val cyclingDistanceKm: Double? = null,
    val workoutSets: Int? = null,
    val activityScore: Double = 0.0,
    val activityAchievement: Double = 0.0,
    val currentWeightKg: Double? = null,
    val weightProgress: Double? = null,
    val daysUntilDeadline: Long = 0,
    val isWeightOverdue: Boolean = false,
)

class DashboardViewModel(
    private val repository: MetricsRepository,
    private val today: LocalDate,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState(date = today))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Bumped on every reload, so a slow, out-of-order refresh never clobbers a newer one.
    private var generation = 0

    init {
        refresh()
    }

    fun onReload() {
        refresh()
    }

    private fun refresh() {
        val requestGeneration = ++generation
        viewModelScope.launch {
            val activityValues = repository.findEffectiveByDate(today)
            val score =
                activityScore(activityValues.steps, activityValues.cyclingDistanceKm, activityValues.workoutSets)
            val currentWeightKg = repository.findLatestWeightKgOnOrBefore(today)
            val weightProgress = currentWeightKg?.let { weightGoalProgress(it) }
            if (requestGeneration != generation) return@launch

            _uiState.value =
                DashboardUiState(
                    date = today,
                    steps = activityValues.steps,
                    cyclingDistanceKm = activityValues.cyclingDistanceKm,
                    workoutSets = activityValues.workoutSets,
                    activityScore = score,
                    activityAchievement = dailyScoreAchievement(score),
                    currentWeightKg = currentWeightKg,
                    weightProgress = weightProgress,
                    daysUntilDeadline = daysUntilWeightDeadline(today),
                    isWeightOverdue = weightProgress?.let { isWeightGoalOverdue(today, it) } ?: false,
                )
        }
    }
}
