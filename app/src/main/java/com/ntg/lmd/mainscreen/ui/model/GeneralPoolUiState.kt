package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class GeneralPoolUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderInfo> = emptyList(),
    val selected: OrderInfo? = null,
    val hasLocationPerm: Boolean = false,
    val distanceThresholdKm: Double = 100.0,
    val searching: Boolean = false,
    override val distanceThresholdKm: Double = 100.0,
    val orders: List<OrderInfo> = emptyList(),
    val searchText: String = "",
    val errorMessage: String? = null,
) {
    val searching: Boolean = false,
    override val selected: OrderInfo? = null,
) : MapUiState {
    companion object {
        private const val MAX_LATITUDE = 90.0
        private const val MAX_LONGITUDE = 180.0
        private const val DISTANCE_EPSILON = 1e-3f
    }

    val filteredOrders: List<OrderInfo>
        get() {
            val q = searchText.trim()
            if (q.isBlank()) return emptyList()
            return orders.filter { o ->
                o.orderNumber.contains(q, ignoreCase = true) ||
                    o.name.contains(q, ignoreCase = true)
            }
        }

    // orders that are within the selected distance
    val mapOrders: List<OrderInfo>
        get() {
            // keep only valid coords
            val base =
    override val mapOrders: List<OrderInfo>
        get() =
            if (distanceThresholdKm <= 0.0) {
                emptyList()
            } else {
                orders.filter {
                    it.lat.isFinite() &&
                        it.lng.isFinite() &&
                        !(it.lat == 0.0 && it.lng == 0.0) &&
                        kotlin.math.abs(it.lat) <= MAX_LATITUDE &&
                        kotlin.math.abs(it.lng) <= MAX_LONGITUDE
                }

            if (!hasLocationPerm) return base

            val anyFinite = base.any { it.distanceKm.isFinite() }
            if (!anyFinite) return emptyList()

            return base.filter { it.distanceKm.isFinite() && it.distanceKm <= distanceThresholdKm }
        }

    // orders that match the current search text
    val filteredOrdersInRange: List<OrderInfo>
        get() {
            val q = searchText.trim()
            val base = mapOrders
            if (q.isBlank()) return base
            return base.filter {
                it.orderNumber.contains(q, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true)
            }
        }
}
