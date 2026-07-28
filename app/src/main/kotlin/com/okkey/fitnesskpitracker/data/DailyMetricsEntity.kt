package com.okkey.fitnesskpitracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_metrics")
data class DailyMetricsEntity(
    @PrimaryKey
    val date: LocalDate,
    val stepsHealthConnect: Long?,
    val stepsManual: Long?,
    val cyclingDistanceKmHealthConnect: Double?,
    val cyclingDistanceKmManual: Double?,
    val weightKgHealthConnect: Double?,
    val weightKgManual: Double?,
    val workoutSets: Int?,
)
