package com.ntg.lmd.mainscreen.domain.usecase

import android.content.Context
import android.location.Location
import com.ntg.lmd.mainscreen.domain.repository.LocationRepository

class GetDeviceLocationsUseCase(
    private val locationRepo: LocationRepository
) {
    suspend operator fun invoke(context: Context): Pair<Location?, Location?> {
        val last = locationRepo.getLastLocation(context)
        val current = locationRepo.getCurrentLocation(context)
        return last to current
    }
}
