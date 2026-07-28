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

    @Insert
    suspend fun insert(entity: DailyMetricsEntity)

    @Update
    suspend fun update(entity: DailyMetricsEntity)

    @Transaction
    suspend fun upsertHealthConnect(
        date: LocalDate,
        stepsHealthConnect: Long?,
        cyclingDistanceKmHealthConnect: Double?,
        weightKgHealthConnect: Double?,
    ) {
        val existing = findByDate(date)
        if (existing == null) {
            insert(
                DailyMetricsEntity(
                    date = date,
                    stepsHealthConnect = stepsHealthConnect,
                    stepsManual = null,
                    cyclingDistanceKmHealthConnect = cyclingDistanceKmHealthConnect,
                    cyclingDistanceKmManual = null,
                    weightKgHealthConnect = weightKgHealthConnect,
                    weightKgManual = null,
                    workoutSets = null,
                ),
            )
        } else {
            update(
                existing.copy(
                    stepsHealthConnect = stepsHealthConnect,
                    cyclingDistanceKmHealthConnect = cyclingDistanceKmHealthConnect,
                    weightKgHealthConnect = weightKgHealthConnect,
                ),
            )
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
