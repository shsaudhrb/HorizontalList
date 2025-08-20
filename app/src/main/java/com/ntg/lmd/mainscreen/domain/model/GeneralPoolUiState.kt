package com.ntg.lmd.mainscreen.domain.model

data class GeneralPoolUiState(
    val isLoading: Boolean = true,
    val hasLocationPerm: Boolean = false,
    val distanceThresholdKm: Float = 0f,
    val orders: List<OrderInfo> = emptyList(),
    val query: String = "",
    val searching: Boolean = false,
    val selected: OrderInfo? = null,
) {
    val filteredOrders: List<OrderInfo>
        get() {
            val q = query.trim()
            if (q.isBlank()) return emptyList()
            return orders.filter { o ->
                o.orderNumber.contains(q, ignoreCase = true) ||
                    o.name.contains(q, ignoreCase = true)
            }
        }

    val mapOrders: List<OrderInfo>
        get() =
            if (distanceThresholdKm <= 0f) {
                emptyList()
            } else {
                orders.filter { it.distanceKm.isFinite() && it.distanceKm <= distanceThresholdKm + 1e-3 }
            }
}
