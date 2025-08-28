package com.ntg.lmd.mainscreen.domain.usecase

import android.location.Location
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import kotlin.math.abs

class ComputeDistancesUseCase {
    companion object {
        // meters -> kilometers
        private const val METERS_IN_KM = 1000.0

        // Geographic coordinate bounds
        private const val MAX_LATITUDE = 90.0
        private const val MAX_LONGITUDE = 180.0
    }

    // coordinates validation
    private fun isValidLatLng(
        lat: Double,
        lng: Double,
    ): Boolean {
        val finite = lat.isFinite() && lng.isFinite()
        val nonZeroPair = !(lat == 0.0 && lng == 0.0)
        val withinBounds = abs(lat) <= MAX_LATITUDE && abs(lng) <= MAX_LONGITUDE
        return finite && nonZeroPair && withinBounds
    }

    // for calculating distance between two coordinates (in km)
    private fun distanceKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double =
        try {
            val result = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, result)
            val meters = result.getOrNull(0)?.toDouble() ?: Double.POSITIVE_INFINITY
            if (meters.isFinite() && meters >= 0.0) meters / METERS_IN_KM else Double.POSITIVE_INFINITY
        } catch (_: Exception) {
            Double.POSITIVE_INFINITY
        }

    // Compute distances to each order and return a new list sorted by distance km
    operator fun invoke(
        origin: Location,
        orders: List<OrderInfo>,
    ): List<OrderInfo> {
        if (orders.isEmpty()) return orders

        return orders
            .map { o ->
                val distKm =
                    if (isValidLatLng(o.lat, o.lng)) {
                        distanceKm(origin.latitude, origin.longitude, o.lat, o.lng)
                    } else {
                        // Unknown/invalid coordinates are pushed to the end
                        Double.POSITIVE_INFINITY
                    }
                // return a copy with computed distance
                o.copy(distanceKm = distKm)
            }
            // nearest first
            .sortedBy { it.distanceKm }
    }
}
