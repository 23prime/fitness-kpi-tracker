package com.okkey.fitnesskpitracker.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period

val HEALTH_CONNECT_PERMISSIONS: Set<String> =
    setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

enum class HealthConnectAvailability { AVAILABLE, UNAVAILABLE }

data class WeightSample(
    val instant: Instant,
    val weightKg: Double,
)

interface HealthConnectGateway {
    suspend fun availability(): HealthConnectAvailability

    suspend fun grantedPermissions(): Set<String>

    suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<LocalDate, Long>

    suspend fun readWeightSamples(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WeightSample>
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

    override suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<LocalDate, Long> {
        val client = HealthConnectClient.getOrCreate(context)
        val groups =
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            LocalDateTime.of(startDate, LocalTime.MIN),
                            LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN),
                        ),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            )
        return groups
            .mapNotNull { group ->
                val total = group.result[StepsRecord.COUNT_TOTAL] ?: return@mapNotNull null
                group.startTime.toLocalDate() to total
            }.toMap()
    }

    override suspend fun readWeightSamples(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WeightSample> {
        val client = HealthConnectClient.getOrCreate(context)
        val timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.of(startDate, LocalTime.MIN),
                LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN),
            )
        val records = mutableListOf<WeightRecord>()
        var pageToken: String? = null
        do {
            val response =
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = WeightRecord::class,
                        timeRangeFilter = timeRangeFilter,
                        pageToken = pageToken,
                    ),
                )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records.map { WeightSample(instant = it.time, weightKg = it.weight.inKilograms) }
    }
}
