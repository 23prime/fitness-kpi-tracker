package com.okkey.fitnesskpitracker.data

import kotlinx.coroutines.flow.first
import java.time.LocalDate

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

class MetricsRepository(
    private val dao: DailyMetricsDao,
) {
    suspend fun findEffectiveByDate(date: LocalDate): DailyMetricsValues {
        val entity = dao.findByDate(date)
        return DailyMetricsValues(
            steps = entity?.stepsManual ?: entity?.stepsHealthConnect,
            cyclingDistanceKm = entity?.cyclingDistanceKmManual ?: entity?.cyclingDistanceKmHealthConnect,
            weightKg = entity?.weightKgManual ?: entity?.weightKgHealthConnect,
            workoutSets = entity?.workoutSets,
        )
    }

    suspend fun findLatestWeightKgOnOrBefore(date: LocalDate): Double? {
        val entity = dao.findLatestWithWeightOnOrBefore(date)
        return entity?.weightKgManual ?: entity?.weightKgHealthConnect
    }

    suspend fun findEarliestDate(): LocalDate? = dao.findEarliestDate()

    suspend fun findWeightRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WeightPoint> =
        dao.observeByDateRange(startDate, endDate).first().mapNotNull { entity ->
            val weightKg = entity.weightKgManual ?: entity.weightKgHealthConnect
            weightKg?.let { WeightPoint(entity.date, it) }
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
}
