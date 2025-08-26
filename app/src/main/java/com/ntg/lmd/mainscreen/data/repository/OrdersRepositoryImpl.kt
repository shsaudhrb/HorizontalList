package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import kotlinx.coroutines.delay

class OrdersRepositoryImpl : OrdersRepository {
    override suspend fun getOrders(): List<OrderInfo> {
        delay(500) // simulate network
     /*   return listOf(
            OrderInfo("181818", "Hanan", 30.0444, 31.2357, OrderStatus.CONFIRMED, 100.0),
            OrderInfo("181819", "Ali", 29.965, 31.25, OrderStatus.NEW, 75.5),
            OrderInfo("181820", "Sara", 30.05, 31.28, OrderStatus.ON_ROUTE, 120.0),
            OrderInfo("181821", "Omar", 30.1, 31.3, OrderStatus.PICKED, 200.0),
        )*/
        return TODO("Provide the return value")
    }
}