package com.okkey.fitnesskpitracker.data

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
