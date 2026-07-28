package com.okkey.fitnesskpitracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DailyMetricsEntity::class], version = 1, exportSchema = true)
@TypeConverters(LocalDateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyMetricsDao(): DailyMetricsDao
}
