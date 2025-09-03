package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.mapper.toDomain
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.model.OrderStatus
import com.ntg.lmd.mainscreen.domain.model.OrdersPage
import com.ntg.lmd.mainscreen.domain.repository.MyOrdersRepository

class MyOrdersRepositoryImpl(
    private val api: OrdersApi,
) : MyOrdersRepository {
    private var cachedPage: Int? = null
    private var cachedLimit: Int? = null
    private var cachedPageData: OrdersPage? = null

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
        assignedAgentId: String?,
        userOrdersOnly: Boolean?
    ): OrdersPage {
        if (isCacheValid(page, limit, bypassCache)) {
            return cachedPageData!!
        }
        val env = api.getOrders(
            page = page,
            limit = limit,
            assignedAgentId = assignedAgentId,
            userOrdersOnly = userOrdersOnly
        )
        if (!env.success) error(env.error ?: "Unknown error from orders-list")

        val raw = env.data?.orders.orEmpty()
        val filtered: List<OrderInfo> =
            raw
                .filter { dto ->
                    dto.statusId?.let { it in allowedIds } ?: (
                            dto.orderstatuses
                                ?.statusName
                                ?.trim()
                                ?.lowercase() in allowedNames
                            )
                }.map { it.toDomain() }

        val pageData = OrdersPage(items = filtered, rawCount = raw.size)
        updateCache(page, limit, pageData)
        return pageData
    }

    fun clearCache() {
        cachedPage = null
        cachedLimit = null
        cachedPageData = null
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
                cachedPageData != null

    private fun updateCache(
        page: Int,
        limit: Int,
        data: OrdersPage,
    ) {
        cachedPage = page
        cachedLimit = limit
        cachedPageData = data
    }
}
