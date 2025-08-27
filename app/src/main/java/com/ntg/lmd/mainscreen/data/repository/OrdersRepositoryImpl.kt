package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.mapper.toDomain
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository

class OrdersRepositoryImpl(
    private val api: OrdersApi,
) : OrdersRepository {
    override suspend fun getOrders(
        page: Int,
        limit: Int,
    ): List<OrderInfo> {
        val env = api.getOrders(page = page, limit = limit)
        if (!env.success) error(env.error ?: "Unknown error from orders-list")
        return env.data
            ?.orders
            .orEmpty()
            .map { it.toDomain() }
    }
}
