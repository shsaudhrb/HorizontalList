package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.domain.model.OrderInfo

data class GeneralPoolUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val orders: List<OrderInfo> = emptyList(),
    override val hasLocationPerm: Boolean = false,
    override val distanceThresholdKm: Double = 100.0,
    val searchText: String = "",
    val errorMessage: String? = null,
    override val selected: OrderInfo? = null,
    val searching: Boolean = false,
) : MapUiState {
    companion object {
        private const val MAX_LATITUDE = 90.0
        private const val MAX_LONGITUDE = 180.0
    }

    // orders that are within the selected distance
    override val mapOrders: List<OrderInfo>
        get() {
            val base =
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

    // orders that are both in range AND match search text
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
