package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okkey.fitnesskpitracker.data.DailyActivityScorePoint
import com.okkey.fitnesskpitracker.data.HEALTH_CONNECT_PERMISSIONS
import com.okkey.fitnesskpitracker.data.HealthConnectAvailability
import com.okkey.fitnesskpitracker.data.HealthConnectGateway
import com.okkey.fitnesskpitracker.data.MetricsRepository
import com.okkey.fitnesskpitracker.data.WeightPoint
import com.okkey.fitnesskpitracker.domain.RollingWindowEvaluation
import com.okkey.fitnesskpitracker.domain.WEIGHT_DEADLINE
import com.okkey.fitnesskpitracker.domain.WEIGHT_START_DATE
import com.okkey.fitnesskpitracker.domain.activityScore
import com.okkey.fitnesskpitracker.domain.activityScoreHistoryWindowStart
import com.okkey.fitnesskpitracker.domain.daysUntilWeightDeadline
import com.okkey.fitnesskpitracker.domain.evaluateRollingWindow
import com.okkey.fitnesskpitracker.domain.hasRollingWindowData
import com.okkey.fitnesskpitracker.domain.isWeightGoalOverdue
import com.okkey.fitnesskpitracker.domain.weightGoalProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class HealthConnectBannerState { NONE, REQUEST_PERMISSION, UNAVAILABLE }

data class DashboardUiState(
    val date: LocalDate,
    val steps: Long? = null,
    val cyclingDistanceKm: Double? = null,
    val workoutSets: Int? = null,
    val activityScore: Double = 0.0,
    val activityScoreHistory: List<DailyActivityScorePoint> = emptyList(),
    val activityRollingWindow: RollingWindowEvaluation? = null,
    val isSelectedDateToday: Boolean = false,
    val canGoToPreviousDay: Boolean = false,
    val canGoToNextDay: Boolean = false,
    val currentWeightKg: Double? = null,
    val weightProgress: Double? = null,
    val daysUntilDeadline: Long = 0,
    val isWeightOverdue: Boolean = false,
    val weightHistory: List<WeightPoint> = emptyList(),
    val healthConnectBannerState: HealthConnectBannerState = HealthConnectBannerState.NONE,
    val isSyncing: Boolean = false,
)

class DashboardViewModel(
    private val repository: MetricsRepository,
    private val healthConnectGateway: HealthConnectGateway,
    private val today: () -> LocalDate,
) : ViewModel() {
    private var selectedDate: LocalDate = today()
    private val _uiState = MutableStateFlow(DashboardUiState(date = selectedDate))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _permissionDeniedEvent = MutableSharedFlow<Unit>()
    val permissionDeniedEvent: SharedFlow<Unit> = _permissionDeniedEvent.asSharedFlow()

    private val _syncFailedEvent = MutableSharedFlow<Unit>()
    val syncFailedEvent: SharedFlow<Unit> = _syncFailedEvent.asSharedFlow()

    // Bumped on every reload, so a slow, out-of-order refresh never clobbers a newer one.
    private var generation = 0

    // Set for the duration of onManualRefresh(), so an overlapping onResume() refresh
    // doesn't clear the loading indicator early by rebuilding state with isSyncing's default.
    private var isManualRefreshing = false

    init {
        refresh()
    }

    fun onReload() {
        refresh()
    }

    // Silent sync for ON_RESUME: no loading indicator, no failure snackbar.
    fun onResume() {
        viewModelScope.launch {
            trySyncHealthConnect()
            refreshSuspend()
        }
    }

    fun onManualRefresh() {
        viewModelScope.launch {
            isManualRefreshing = true
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                val succeeded = trySyncHealthConnect()
                refreshSuspend()
                if (!succeeded) _syncFailedEvent.emit(Unit)
            } finally {
                isManualRefreshing = false
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }

    private suspend fun trySyncHealthConnect(): Boolean =
        runCatching { repository.syncHealthConnect(healthConnectGateway, today()) }
            .onFailure { if (it is CancellationException) throw it }
            .getOrDefault(false)

    fun onPermissionResult(grantedPermissions: Set<String>) {
        if (HEALTH_CONNECT_PERMISSIONS.none { it in grantedPermissions }) {
            viewModelScope.launch { _permissionDeniedEvent.emit(Unit) }
        }
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
        viewModelScope.launch { refreshSuspend() }
    }

    private suspend fun refreshSuspend() {
        val requestGeneration = ++generation
        val date = selectedDate
        val todayDate = today()
        val activityValues = repository.findEffectiveByDate(date)
        val score =
            activityScore(activityValues.steps, activityValues.cyclingDistanceKm, activityValues.workoutSets)
        val activityScoreHistory =
            repository.findActivityScoreRange(activityScoreHistoryWindowStart(date), date)
        val rollingWindow =
            if (hasRollingWindowData(activityScoreHistory.map { it.score })) {
                val otherDaysScores = activityScoreHistory.mapNotNull { if (it.date == date) null else it.score }
                evaluateRollingWindow(otherDaysScores, score)
            } else {
                null
            }
        val earliestDate = repository.findEarliestDate() ?: date
        val currentWeightKg = repository.findLatestWeightKgOnOrBefore(todayDate)
        val weightProgress = currentWeightKg?.let { weightGoalProgress(it) }
        val weightHistory = repository.findWeightRange(WEIGHT_START_DATE, WEIGHT_DEADLINE)
        val bannerState = healthConnectBannerState()
        if (requestGeneration != generation) return

        _uiState.value =
            DashboardUiState(
                date = date,
                steps = activityValues.steps,
                cyclingDistanceKm = activityValues.cyclingDistanceKm,
                workoutSets = activityValues.workoutSets,
                activityScore = score,
                activityScoreHistory = activityScoreHistory,
                activityRollingWindow = rollingWindow,
                isSelectedDateToday = date == todayDate,
                canGoToPreviousDay = date > earliestDate,
                canGoToNextDay = date < todayDate,
                currentWeightKg = currentWeightKg,
                weightProgress = weightProgress,
                daysUntilDeadline = daysUntilWeightDeadline(todayDate),
                isWeightOverdue = weightProgress?.let { isWeightGoalOverdue(todayDate, it) } ?: false,
                weightHistory = weightHistory,
                healthConnectBannerState = bannerState,
                isSyncing = isManualRefreshing,
            )
    }

    private suspend fun healthConnectBannerState(): HealthConnectBannerState {
        if (healthConnectGateway.availability() != HealthConnectAvailability.AVAILABLE) {
            return HealthConnectBannerState.UNAVAILABLE
        }
        val granted = healthConnectGateway.grantedPermissions()
        return if (HEALTH_CONNECT_PERMISSIONS.any { it in granted }) {
            HealthConnectBannerState.NONE
        } else {
            HealthConnectBannerState.REQUEST_PERMISSION
        }
    }
}
