package com.ntg.lmd.mainscreen.domain.model
/// Renad

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
    val customerPhone:String?,
    val details:String?
)

enum class OrderStatus {
    ADDED,
    CONFIRMED,
    CANCELED,
    REASSIGNED,
    PICKUP,
    START_DELIVERY,
    DELIVERY_FAILED,
    DELIVERY_DONE,
}
