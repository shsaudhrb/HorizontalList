package com.ntg.lmd.mainscreen.domain.model

import com.ntg.lmd.order.domain.model.OrderStatusCode

const val STATUS_ADDED = 1
const val STATUS_CONFIRMED = 2
const val STATUS_REASSIGNED = 4
const val STATUS_PICKUP = 5
const val STATUS_START_DELIVERY = 6

data class OrderInfo(
    val id: String = "",
    val name: String = "",
    val orderNumber: String = "",
    val timeAgo: String = "0m ago",
    val itemsCount: Int = 0,
    val distanceKm: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: OrderStatus? = OrderStatus.ADDED,
    val price: String = "---",
    val customerPhone: String? = "",
    val details: String?,
    val customerId: String? = "",
    val assignedAgentId: String? = "",
)

enum class OrderStatus(
    val id: Int,
) {
    ADDED(STATUS_ADDED),
    CONFIRMED(STATUS_CONFIRMED),
    CANCELED(OrderStatusCode.CANCELLED.code),
    REASSIGNED(STATUS_REASSIGNED),
    PICKUP(STATUS_PICKUP),
    START_DELIVERY(STATUS_START_DELIVERY),
    DELIVERY_FAILED(OrderStatusCode.FAILED.code),
    DELIVERY_DONE(OrderStatusCode.DONE.code),
    ;

    companion object {
        fun fromId(id: Int?): OrderStatus? = values().firstOrNull { it.id == id }
    }
}
fun OrderStatus.toApiId(): Int =
    when (this) {
        OrderStatus.ADDED -> STATUS_ADDED
        OrderStatus.CONFIRMED -> STATUS_CONFIRMED
        OrderStatus.CANCELED -> OrderStatusCode.CANCELLED.code
        OrderStatus.REASSIGNED -> STATUS_REASSIGNED
        OrderStatus.PICKUP -> STATUS_PICKUP
        OrderStatus.START_DELIVERY -> STATUS_START_DELIVERY
        OrderStatus.DELIVERY_FAILED -> OrderStatusCode.FAILED.code
        OrderStatus.DELIVERY_DONE -> OrderStatusCode.DONE.code
    }
