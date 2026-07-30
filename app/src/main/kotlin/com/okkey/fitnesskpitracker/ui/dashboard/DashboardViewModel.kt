package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.data.WeightPoint
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_START_DATE
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
    val canGoToPreviousDay: Boolean = false,
    val canGoToNextDay: Boolean = false,
    val currentWeightKg: Double? = null,
    val weightProgress: Double? = null,
    val daysUntilDeadline: Long = 0,
    val isWeightOverdue: Boolean = false,
    val weightHistory: List<WeightPoint> = emptyList(),
)

class DashboardViewModel(
    private val repository: MetricsRepository,
    private val today: () -> LocalDate,
) : ViewModel() {
    private var selectedDate: LocalDate = today()
    private val _uiState = MutableStateFlow(DashboardUiState(date = selectedDate))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Bumped on every reload, so a slow, out-of-order refresh never clobbers a newer one.
    private var generation = 0

    init {
        refresh()
    }

    fun onReload() {
        refresh()
    }

    fun onPreviousDay() {
        val state = _uiState.value
        if (state.date != selectedDate || !state.canGoToPreviousDay) return
        selectedDate = selectedDate.minusDays(1)
        refresh()
    }

    fun onNextDay() {
        val state = _uiState.value
        if (state.date != selectedDate || !state.canGoToNextDay) return
        selectedDate = selectedDate.plusDays(1)
        refresh()
    }

    private fun refresh() {
        val requestGeneration = ++generation
        val date = selectedDate
        viewModelScope.launch {
            val todayDate = today()
            val activityValues = repository.findEffectiveByDate(date)
            val score =
                activityScore(activityValues.steps, activityValues.cyclingDistanceKm, activityValues.workoutSets)
            val earliestDate = repository.findEarliestDate() ?: date
            val currentWeightKg = repository.findLatestWeightKgOnOrBefore(todayDate)
            val weightProgress = currentWeightKg?.let { weightGoalProgress(it) }
            val weightHistory = repository.findWeightRange(WEIGHT_START_DATE, WEIGHT_DEADLINE)
            if (requestGeneration != generation) return@launch

            _uiState.value =
                DashboardUiState(
                    date = date,
                    steps = activityValues.steps,
                    cyclingDistanceKm = activityValues.cyclingDistanceKm,
                    workoutSets = activityValues.workoutSets,
                    activityScore = score,
                    activityAchievement = dailyScoreAchievement(score),
                    canGoToPreviousDay = date > earliestDate,
                    canGoToNextDay = date < todayDate,
                    currentWeightKg = currentWeightKg,
                    weightProgress = weightProgress,
                    daysUntilDeadline = daysUntilWeightDeadline(todayDate),
                    isWeightOverdue = weightProgress?.let { isWeightGoalOverdue(todayDate, it) } ?: false,
                    weightHistory = weightHistory,
                )
        }
    }
}
