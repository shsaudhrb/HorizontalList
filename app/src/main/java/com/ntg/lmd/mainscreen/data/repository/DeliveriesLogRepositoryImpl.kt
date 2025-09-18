package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.toDeliveryLog
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository

class DeliveriesLogRepositoryImpl(
    private val api: OrdersApi,
) : DeliveriesLogRepository {
    override suspend fun getLogsPage(
        page: Int,
        limit: Int,
        statusIds: List<Int>,
        search: String?,
    ): Pair<List<DeliveryLog>, Boolean> {
        val statusFilter = if (statusIds.isEmpty()) null else statusIds.joinToString(",")
        val env =
            api.getOrders(
                page = page,
                limit = limit,
                statusIds = statusFilter,
                search = search,
                assignedAgentId = null,
                userOrdersOnly = false,
            )
        if (!env.success) error(env.error ?: "Unknown error from orders-list")
        val data = env.data ?: return emptyList<DeliveryLog>() to false

        val logs =
            data.orders
                .map { it.toDeliveryLog() }

        val hasNext = data.pagination?.hasNextPage ?: false
        return logs to hasNext
    }
}
