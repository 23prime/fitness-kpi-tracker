package com.okkey.fitnesskpitracker.data

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import com.okkey.fitnesskpitracker.domain.activityScore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private const val SYNC_RANGE_DAYS = 30L

enum class ManualField {
    STEPS,
    CYCLING_DISTANCE,
    WEIGHT,
    WORKOUT_SETS,
}

data class DailyMetricsValues(
    val steps: Long?,
    val cyclingDistanceKm: Double?,
    val weightKg: Double?,
    val workoutSets: Int?,
)

data class WeightPoint(
    val date: LocalDate,
    val weightKg: Double,
)

data class DailyActivityScorePoint(
    val date: LocalDate,
    // Null means no activity was recorded for this date (no row, or a row with only
    // weight data), distinct from a recorded score of 0.
    val score: Double?,
)

class MetricsRepository(
    private val dao: DailyMetricsDao,
) {
    suspend fun findEffectiveByDate(date: LocalDate): DailyMetricsValues {
        val (manual, healthConnect) = findSplitByDate(date)
        return DailyMetricsValues(
            steps = manual.steps ?: healthConnect.steps,
            cyclingDistanceKm = manual.cyclingDistanceKm ?: healthConnect.cyclingDistanceKm,
            weightKg = manual.weightKg ?: healthConnect.weightKg,
            workoutSets = manual.workoutSets,
        )
    }

    suspend fun findSplitByDate(date: LocalDate): Pair<DailyMetricsValues, DailyMetricsValues> {
        val entity = dao.findByDate(date)
        val manual =
            DailyMetricsValues(
                steps = entity?.stepsManual,
                cyclingDistanceKm = entity?.cyclingDistanceKmManual,
                weightKg = entity?.weightKgManual,
                workoutSets = entity?.workoutSets,
            )
        val healthConnect =
            DailyMetricsValues(
                steps = entity?.stepsHealthConnect,
                cyclingDistanceKm = entity?.cyclingDistanceKmHealthConnect,
                weightKg = entity?.weightKgHealthConnect,
                workoutSets = null,
            )
        return manual to healthConnect
    }

    suspend fun findLatestWeightKgOnOrBefore(date: LocalDate): Double? {
        val entity = dao.findLatestWithWeightOnOrBefore(date)
        return entity?.weightKgManual ?: entity?.weightKgHealthConnect
    }

    suspend fun findEarliestDate(): LocalDate? = dao.findEarliestDate()

    suspend fun exportCsv(): String = MetricsCsv.format(dao.findAll())

    suspend fun findWeightRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WeightPoint> =
        dao.observeByDateRange(startDate, endDate).first().mapNotNull { entity ->
            val weightKg = entity.weightKgManual ?: entity.weightKgHealthConnect
            weightKg?.let { WeightPoint(entity.date, it) }
        }

    suspend fun findActivityScoreRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailyActivityScorePoint> {
        val entitiesByDate = dao.observeByDateRange(startDate, endDate).first().associateBy { it.date }
        return generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .map { date -> DailyActivityScorePoint(date, entitiesByDate[date]?.let(::effectiveActivityScoreOrNull)) }
            .toList()
    }

    // Null when the day has no recorded activity at all, so callers can distinguish
    // "not recorded" from a genuine 0 pt day (e.g. Health Connect wrote 0 steps).
    private fun effectiveActivityScoreOrNull(entity: DailyMetricsEntity): Double? {
        val steps = entity.stepsManual ?: entity.stepsHealthConnect
        val cyclingDistanceKm = entity.cyclingDistanceKmManual ?: entity.cyclingDistanceKmHealthConnect
        val workoutSets = entity.workoutSets
        if (steps == null && cyclingDistanceKm == null && workoutSets == null) return null
        return activityScore(steps, cyclingDistanceKm, workoutSets)
    }

    suspend fun saveManual(
        date: LocalDate,
        steps: Long?,
        cyclingDistanceKm: Double?,
        weightKg: Double?,
        workoutSets: Int?,
    ) {
        dao.upsertManual(
            date,
            stepsManual = steps,
            cyclingDistanceKmManual = cyclingDistanceKm,
            weightKgManual = weightKg,
            workoutSets = workoutSets,
        )
    }

    // Returns false if a granted category failed to read, so the caller can surface a failure
    // without losing already-synced data (fields without permission or unreadable are left untouched).
    suspend fun syncHealthConnect(
        gateway: HealthConnectGateway,
        today: LocalDate,
    ): Boolean {
        val startDate = today.minusDays(SYNC_RANGE_DAYS - 1)
        val granted = gateway.grantedPermissions()
        val stepsGranted = HealthPermission.getReadPermission(StepsRecord::class) in granted
        val weightGranted = HealthPermission.getReadPermission(WeightRecord::class) in granted
        if (!stepsGranted && !weightGranted) return true

        var succeeded = true

        val dailySteps =
            if (stepsGranted) {
                runCatching { gateway.readDailySteps(startDate, today) }
                    .onFailure {
                        if (it is CancellationException) throw it
                        succeeded = false
                    }.getOrNull()
            } else {
                null
            }

        val dailyWeight =
            if (weightGranted) {
                runCatching { gateway.readWeightSamples(startDate, today) }
                    .mapCatching { latestWeightPerDate(it) }
                    .onFailure {
                        if (it is CancellationException) throw it
                        succeeded = false
                    }.getOrNull()
            } else {
                null
            }

        var date = startDate
        while (!date.isAfter(today)) {
            val stepsUpdate =
                if (stepsGranted && dailySteps != null) {
                    HealthConnectFieldUpdate.Write(dailySteps[date] ?: 0L)
                } else {
                    HealthConnectFieldUpdate.Skip
                }
            val weightUpdate =
                if (weightGranted && dailyWeight != null) {
                    HealthConnectFieldUpdate.Write(dailyWeight[date])
                } else {
                    HealthConnectFieldUpdate.Skip
                }
            dao.upsertHealthConnect(date, stepsUpdate, weightUpdate)
            date = date.plusDays(1)
        }

        return succeeded
    }
}
