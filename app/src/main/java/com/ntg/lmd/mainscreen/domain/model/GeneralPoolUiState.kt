package com.ntg.lmd.mainscreen.domain.model

data class GeneralPoolUiState(
    val isLoading: Boolean = true,
    val hasLocationPerm: Boolean = false,
    val distanceThresholdKm: Float = 0f,
    val orders: List<OrderInfo> = emptyList(),
    val searchText: String = "",
    val searching: Boolean = false,
    val selected: OrderInfo? = null,
) {
    companion object {
        private const val DISTANCE_EPSILON = 1e-3f // ~1 meter
    }

    // orders that match the current search text
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
        get() =
            if (distanceThresholdKm <= 0f) {
                emptyList()
            } else {
                orders.filter { it.distanceKm.isFinite() && it.distanceKm <= distanceThresholdKm + DISTANCE_EPSILON }
            }
}
