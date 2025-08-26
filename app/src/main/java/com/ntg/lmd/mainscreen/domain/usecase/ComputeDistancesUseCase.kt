package com.ntg.lmd.mainscreen.domain.usecase

import android.location.Location
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

class ComputeDistancesUseCase {

    // meters -> kilometers
    companion object {
        private const val METERS_IN_KM = 1000.0
    }

    // for calculating distance between two coordinates (in km)
    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0] / METERS_IN_KM
    }

    // computer distance from origin to each order and return a new sorted list (nearest first)
    operator fun invoke(origin: Location, orders: List<OrderInfo>): List<OrderInfo> {
        return orders
            .map { o ->
                o.copy(distanceKm = distanceKm(origin.latitude, origin.longitude, o.lat, o.lng))
            }
            .sortedBy { it.distanceKm }
    }
}
