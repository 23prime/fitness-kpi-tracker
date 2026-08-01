package com.okkey.fitnesskpitracker

import android.app.Application
import androidx.room.Room
import com.okkey.fitnesskpitracker.data.AppDatabase
import com.okkey.fitnesskpitracker.data.HealthConnectGateway
import com.okkey.fitnesskpitracker.data.HealthConnectGatewayImpl
import com.okkey.fitnesskpitracker.data.MetricsRepository

class FitnessKpiApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "fitness-kpi-tracker.db").build()
    }

    val metricsRepository: MetricsRepository by lazy {
        MetricsRepository(database.dailyMetricsDao())
    }

    val healthConnectGateway: HealthConnectGateway by lazy {
        HealthConnectGatewayImpl(this)
    }
}
