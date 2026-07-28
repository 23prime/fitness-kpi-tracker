package com.okkey.fitnesskpitracker

import android.app.Application
import androidx.room.Room
import com.okkey.fitnesskpitracker.data.AppDatabase

class FitnessKpiApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "fitness-kpi-tracker.db").build()
    }
}
