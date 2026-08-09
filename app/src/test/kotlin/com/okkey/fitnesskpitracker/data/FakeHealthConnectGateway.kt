package com.okkey.fitnesskpitracker.data

import java.time.LocalDate

class FakeHealthConnectGateway(
    var availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    var grantedPermissions: Set<String> = HEALTH_CONNECT_PERMISSIONS,
    var dailySteps: Map<LocalDate, Long> = emptyMap(),
    var weightSamples: List<WeightSample> = emptyList(),
    var readDailyStepsError: Throwable? = null,
    var readWeightSamplesError: Throwable? = null,
) : HealthConnectGateway {
    // Awaited only on the first call to readDailySteps, so a test can pause exactly one
    // in-flight sync (e.g. a manual refresh) while a later, unrelated sync (e.g. a resume)
    // still completes immediately.
    var onFirstReadDailySteps: suspend () -> Unit = {}
    private var readDailyStepsCallCount = 0

    override suspend fun availability(): HealthConnectAvailability = availability

    override suspend fun grantedPermissions(): Set<String> = grantedPermissions

    override suspend fun readDailySteps(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Map<LocalDate, Long> {
        readDailyStepsCallCount++
        if (readDailyStepsCallCount == 1) onFirstReadDailySteps()
        readDailyStepsError?.let { throw it }
        return dailySteps.filterKeys { it in startDate..endDate }
    }

    override suspend fun readWeightSamples(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WeightSample> {
        readWeightSamplesError?.let { throw it }
        val startInstant = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        val endInstant = endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        return weightSamples.filter { it.instant >= startInstant && it.instant < endInstant }
    }
}
