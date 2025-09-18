package com.ntg.lmd.mainscreen.domain.usecase

import android.location.Location
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository

class GetDeviceLocationsUseCase(
    private val locationRepo: LocationRepository,
) {
    suspend operator fun invoke(): Pair<Location?, Location?> {
        val last = locationRepo.getLastLocation()
        val current = locationRepo.getCurrentLocation()
        return last to current
    }
}
