package com.okkey.fitnesskpitracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyMetricsDao {
    @Query("SELECT * FROM daily_metrics WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun observeByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyMetricsEntity>>

    @Query("SELECT * FROM daily_metrics WHERE date = :date")
    suspend fun findByDate(date: LocalDate): DailyMetricsEntity?

    @Query("SELECT MIN(date) FROM daily_metrics")
    suspend fun findEarliestDate(): LocalDate?

    @Query(
        "SELECT * FROM daily_metrics " +
            "WHERE date <= :date AND (weightKgManual IS NOT NULL OR weightKgHealthConnect IS NOT NULL) " +
            "ORDER BY date DESC LIMIT 1",
    )
    suspend fun findLatestWithWeightOnOrBefore(date: LocalDate): DailyMetricsEntity?

    @Insert
    suspend fun insert(entity: DailyMetricsEntity)

    @Update
    suspend fun update(entity: DailyMetricsEntity)

    @Transaction
    suspend fun upsertHealthConnect(
        date: LocalDate,
        steps: HealthConnectFieldUpdate<Long>,
        weightKg: HealthConnectFieldUpdate<Double>,
    ) {
        val existing = findByDate(date)
        val newSteps = (steps as? HealthConnectFieldUpdate.Write)?.value
        val newWeightKg = (weightKg as? HealthConnectFieldUpdate.Write)?.value
        if (existing == null) {
            if (newSteps == null && newWeightKg == null) return
            insert(
                DailyMetricsEntity(
                    date = date,
                    stepsHealthConnect = newSteps,
                    stepsManual = null,
                    cyclingDistanceKmHealthConnect = null,
                    cyclingDistanceKmManual = null,
                    weightKgHealthConnect = newWeightKg,
                    weightKgManual = null,
                    workoutSets = null,
                ),
            )
        } else {
            if (steps is HealthConnectFieldUpdate.Skip && weightKg is HealthConnectFieldUpdate.Skip) return
            val nextSteps = if (steps is HealthConnectFieldUpdate.Write) steps.value else existing.stepsHealthConnect
            val nextWeightKg =
                if (weightKg is HealthConnectFieldUpdate.Write) weightKg.value else existing.weightKgHealthConnect
            update(existing.copy(stepsHealthConnect = nextSteps, weightKgHealthConnect = nextWeightKg))
        }
    }

    @Transaction
    suspend fun upsertManual(
        date: LocalDate,
        stepsManual: Long?,
        cyclingDistanceKmManual: Double?,
        weightKgManual: Double?,
        workoutSets: Int?,
    ) {
        val existing = findByDate(date)
        if (existing == null) {
            insert(
                DailyMetricsEntity(
                    date = date,
                    stepsHealthConnect = null,
                    stepsManual = stepsManual,
                    cyclingDistanceKmHealthConnect = null,
                    cyclingDistanceKmManual = cyclingDistanceKmManual,
                    weightKgHealthConnect = null,
                    weightKgManual = weightKgManual,
                    workoutSets = workoutSets,
                ),
            )
        } else {
            update(
                existing.copy(
                    stepsManual = stepsManual,
                    cyclingDistanceKmManual = cyclingDistanceKmManual,
                    weightKgManual = weightKgManual,
                    workoutSets = workoutSets,
                ),
            )
        }
    }
}
