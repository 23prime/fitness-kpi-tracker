package com.okkey.fitnesskpitracker.data

sealed interface HealthConnectFieldUpdate<out T> {
    data object Skip : HealthConnectFieldUpdate<Nothing>

    data class Write<T>(
        val value: T?,
    ) : HealthConnectFieldUpdate<T>
}
