package com.ntg.lmd.mainscreen.domain.model

import com.ntg.lmd.mainscreen.ui.model.MapUiState

data class GeneralPoolUiState(
    val isLoading: Boolean = true,
    val hasLocationPerm: Boolean = false,
    override val distanceThresholdKm: Double = 100.0,
    val orders: List<OrderInfo> = emptyList(),
    val searchText: String = "",
    val searching: Boolean = false,
    override val selected: OrderInfo? = null,
) : MapUiState {
    companion object {
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

    override val mapOrders: List<OrderInfo>
        get() =
            if (distanceThresholdKm <= 0.0) {
                emptyList()
            } else {
                orders.filter {
                    it.distanceKm.isFinite() && it.distanceKm <= distanceThresholdKm + DISTANCE_EPSILON
                }
            }

    val filteredOrdersInRange: List<OrderInfo>
        get() =
            if (distanceThresholdKm <= 0.0) {
                emptyList()
            } else {
                filteredOrders.filter {
                    it.distanceKm.isFinite() && it.distanceKm <= distanceThresholdKm + DISTANCE_EPSILON
                }
            }
}
