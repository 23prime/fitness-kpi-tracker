package com.okkey.fitnesskpitracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DailyMetricsCsvDao {
    @Query("SELECT * FROM daily_metrics ORDER BY date")
    suspend fun findAll(): List<DailyMetricsEntity>

    @Insert
    suspend fun insertAll(entities: List<DailyMetricsEntity>)

    @Query("DELETE FROM daily_metrics")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<DailyMetricsEntity>) {
        deleteAll()
        insertAll(entities)
    }
}
