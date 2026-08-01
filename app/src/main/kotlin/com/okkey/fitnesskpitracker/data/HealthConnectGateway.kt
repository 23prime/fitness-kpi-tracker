package com.okkey.fitnesskpitracker.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

val HEALTH_CONNECT_PERMISSIONS: Set<String> =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

enum class HealthConnectAvailability { AVAILABLE, UNAVAILABLE }

interface HealthConnectGateway {
    suspend fun availability(): HealthConnectAvailability

    suspend fun grantedPermissions(): Set<String>
}

class HealthConnectGatewayImpl(
    private val context: Context,
) : HealthConnectGateway {
    override suspend fun availability(): HealthConnectAvailability =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectAvailability.AVAILABLE
        } else {
            HealthConnectAvailability.UNAVAILABLE
        }

    override suspend fun grantedPermissions(): Set<String> {
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions()
    }
}
