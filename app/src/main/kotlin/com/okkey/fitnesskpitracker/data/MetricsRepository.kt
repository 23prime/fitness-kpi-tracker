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

    suspend fun clearManualField(
        date: LocalDate,
        field: ManualField,
    ) {
        val existing = dao.findByDate(date) ?: return
        dao.upsertManual(
            date,
            stepsManual = if (field == ManualField.STEPS) null else existing.stepsManual,
            cyclingDistanceKmManual =
                if (field == ManualField.CYCLING_DISTANCE) null else existing.cyclingDistanceKmManual,
            weightKgManual = if (field == ManualField.WEIGHT) null else existing.weightKgManual,
            workoutSets = if (field == ManualField.WORKOUT_SETS) null else existing.workoutSets,
        )
    }
}
