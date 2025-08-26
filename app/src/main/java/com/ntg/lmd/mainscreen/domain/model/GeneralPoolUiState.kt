package com.ntg.lmd.mainscreen.domain.model

data class GeneralPoolUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderInfo> = emptyList(),
    val selected: OrderInfo? = null,

    val hasLocationPerm: Boolean = false,
    val distanceThresholdKm: Double = 100.0,

    val searching: Boolean = false,
    val searchText: String = ""
) {

    // orders that are within the selected distance
    val mapOrders: List<OrderInfo>
        get() {
            if (!hasLocationPerm) return orders
            val anyFinite = orders.any { it.distanceKm.isFinite() }
            if (!anyFinite) return orders

            val filtered = orders.filter { it.distanceKm <= distanceThresholdKm }
            return if (filtered.isEmpty()) orders else filtered
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
