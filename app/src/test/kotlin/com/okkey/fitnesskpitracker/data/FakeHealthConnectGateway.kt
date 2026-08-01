package com.okkey.fitnesskpitracker.data

class FakeHealthConnectGateway(
    var availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    var grantedPermissions: Set<String> = HEALTH_CONNECT_PERMISSIONS,
) : HealthConnectGateway {
    override suspend fun availability(): HealthConnectAvailability = availability

    override suspend fun grantedPermissions(): Set<String> = grantedPermissions
}
