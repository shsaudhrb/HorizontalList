package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.mapper.toDomain
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.model.OrdersPage
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository

class MyOrdersRepositoryImpl(
    private val api: OrdersApi,
) : MyOrdersRepository {
    private var cachedPage: Int? = null
    private var cachedLimit: Int? = null
    private var cachedOrders: List<OrderInfo>? = null

    private val allowedIds =
        setOf(
            OrderStatus.ADDED.id,
            OrderStatus.CONFIRMED.id,
            OrderStatus.REASSIGNED.id,
            OrderStatus.PICKUP.id,
            OrderStatus.START_DELIVERY.id,
        )

    private val allowedNames =
        setOf(
            "added",
            "confirmed",
            "reassigned",
            "pickup",
            "picked",
            "start delivery",
        )

    override suspend fun getOrders(
        page: Int,
        limit: Int,
        bypassCache: Boolean,
    ): OrdersPage {
        if (isCacheValid(page, limit, bypassCache)) {
            return cachedOrders.orEmpty()
        }

        val env = api.getOrders(page = page, limit = limit)

        if (!env.success) {
            error(env.error ?: "Unknown error from orders-list")
        }
        val raw = env.data?.orders.orEmpty()
        val filtered =
            raw
                .filter { dto ->
                    dto.statusId?.let { it in allowedIds }
                        ?: (
                                dto.orderstatuses
                                    ?.statusName
                                    ?.trim()
                                    ?.lowercase() in allowedNames
                                )
                }.map { it.toDomain() }

        return OrdersPage(items = filtered, rawCount = raw.size)
        val orders =
            env.data
                ?.orders
                .orEmpty()
                .map { it.toDomain() }

        updateCache(page, limit, orders)
        return orders
    }

    fun clearCache() {
        cachedPage = null
        cachedLimit = null
        cachedOrders = null
    }

    // ---- Helpers ----

    private fun isCacheValid(
        page: Int,
        limit: Int,
        bypassCache: Boolean,
    ): Boolean =
        !bypassCache &&
            cachedPage == page &&
            cachedLimit == limit &&
            cachedOrders != null

    private fun updateCache(
        page: Int,
        limit: Int,
        orders: List<OrderInfo>,
    ) {
        cachedPage = page
        cachedLimit = limit
        cachedOrders = orders
    }
}
