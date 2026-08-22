package com.okkey.fitnesskpitracker.ui.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.okkey.fitnesskpitracker.R
import com.okkey.fitnesskpitracker.data.HEALTH_CONNECT_PERMISSIONS
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal val DATE_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
private const val PERCENT_SCALE = 100
private val SYNC_INDICATOR_SIZE = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionDeniedMessage = stringResource(R.string.dashboard_permission_denied_message)
    val syncFailedMessage = stringResource(R.string.dashboard_sync_failed_message)

    LaunchedEffect(viewModel) {
        viewModel.permissionDeniedEvent.collect {
            snackbarHostState.showSnackbar(permissionDeniedMessage)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.syncFailedEvent.collect {
            snackbarHostState.showSnackbar(syncFailedMessage)
        }
    }

    ReloadOnResume(viewModel::onResume)

    val permissionLauncher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            viewModel.onPermissionResult(granted)
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    RefreshAction(isSyncing = uiState.isSyncing, onClick = viewModel::onManualRefresh)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            HealthConnectBanner(
                state = uiState.healthConnectBannerState,
                onRequestPermission = { permissionLauncher.launch(HEALTH_CONNECT_PERMISSIONS) },
            )
            ActivityScoreSection(
                uiState = uiState,
                onPreviousDay = viewModel::onPreviousDay,
                onNextDay = viewModel::onNextDay,
                onEvaluationModeChange = viewModel::onActivityScoreEvaluationModeChange,
            )
            WeightGoalSection(uiState)
        }
    }
}

@Composable
private fun RefreshAction(
    isSyncing: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = !isSyncing) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(SYNC_INDICATOR_SIZE))
        } else {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.dashboard_button_reload),
            )
        }
    }
}

@Composable
private fun ReloadOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentOnResume()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun HealthConnectBanner(
    state: HealthConnectBannerState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HealthConnectBannerState.NONE -> {}

        HealthConnectBannerState.REQUEST_PERMISSION -> {
            Card(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_permission_banner_message),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRequestPermission) {
                        Text(stringResource(R.string.dashboard_permission_banner_button))
                    }
                }
            }
        }

        HealthConnectBannerState.UNAVAILABLE -> {
            Card(modifier = modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dashboard_permission_unavailable_message),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

internal fun formatPercent(value: Double): String = "${(value * PERCENT_SCALE).roundToInt()}%"
