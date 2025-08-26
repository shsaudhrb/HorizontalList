package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import kotlinx.coroutines.delay

class OrdersRepositoryImpl : OrdersRepository {
    override suspend fun getOrders(): List<OrderInfo> {
        delay(500) // simulate network
        return listOf(
            OrderInfo(
                orderNumber = "181818",
                name = "Hanan",
                lat = 30.0444,
                lng = 31.2357,
                status = OrderStatus.CONFIRMED,
                price = 100.0,
                timeAgo = "5m ago",
                itemsCount = 3,
                distanceKm = 2.5,
            ),
            OrderInfo(
                orderNumber = "181819",
                name = "Ali",
                lat = 29.965,
                lng = 31.25,
                status = OrderStatus.NEW,
                price = 75.5,
                timeAgo = "10m ago",
                itemsCount = 1,
                distanceKm = 4.2,
            ),
            OrderInfo(
                orderNumber = "181820",
                name = "Sara",
                lat = 30.05,
                lng = 31.28,
                status = OrderStatus.ON_ROUTE,
                price = 120.0,
                timeAgo = "15m ago",
                itemsCount = 5,
                distanceKm = 1.8,
            ),
            OrderInfo(
                orderNumber = "181821",
                name = "Omar",
                lat = 30.1,
                lng = 31.3,
                status = OrderStatus.PICKED,
                price = 200.0,
                timeAgo = "20m ago",
                itemsCount = 2,
                distanceKm = 3.6,
            ),
        )
    }
}
