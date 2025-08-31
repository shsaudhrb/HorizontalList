package com.ntg.lmd.mainscreen.domain.model

const val STATUS_ADDED = 1
const val STATUS_CONFIRMED = 2
const val STATUS_CANCELED = 3
const val STATUS_REASSIGNED = 4
const val STATUS_PICKUP = 5
const val STATUS_START_DELIVERY = 6
const val STATUS_DELIVERY_FAILED = 7
const val STATUS_DELIVERY_DONE = 8

data class OrderInfo(
    val id: String = "",
    val name: String = "",
    val orderNumber: String = "",
    val timeAgo: String = "0m ago",
    val itemsCount: Int = 0,
    val distanceKm: Double = 0.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: OrderStatus = OrderStatus.ADDED,
    val price: String = "---",
    val customerPhone: String?,
    val details: String?,
)

enum class OrderStatus(
    val id: Int,
) {
    ADDED(STATUS_ADDED),
    CONFIRMED(STATUS_CONFIRMED),
    CANCELED(STATUS_CANCELED),
    REASSIGNED(STATUS_REASSIGNED),
    PICKUP(STATUS_PICKUP),
    START_DELIVERY(STATUS_START_DELIVERY),
    DELIVERY_FAILED(STATUS_DELIVERY_FAILED),
    DELIVERY_DONE(STATUS_DELIVERY_DONE),
    ;

    companion object {
        fun fromId(id: Int?): OrderStatus? = values().firstOrNull { it.id == id }
    }
}
