package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.mapper.toDomain
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository

class MyOrdersRepositoryImpl(
    private val api: OrdersApi,
) : MyOrdersRepository {

    private var cachedPage: Int? = null
    private var cachedLimit: Int? = null
    private var cachedOrders: List<OrderInfo>? = null

    override suspend fun getOrders(
        page: Int,
        limit: Int,
        bypassCache: Boolean
    ): List<OrderInfo> {
        if (!bypassCache && cachedPage == page && cachedLimit == limit && cachedOrders != null) {
            return cachedOrders!!
        }
        val env = api.getOrders(page = page, limit = limit)
        if (!env.success) error(env.error ?: "Unknown error from orders-list")
        val list = env.data?.orders.orEmpty().map { it.toDomain() }

        cachedPage = page
        cachedLimit = limit
        cachedOrders = list
        return list
    }

    fun clearCache() {
        cachedPage = null
        cachedLimit = null
        cachedOrders = null
    }
}

