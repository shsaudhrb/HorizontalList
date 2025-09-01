package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.domain.model.DeliveryLog

interface DeliveriesLogRepository {
    suspend fun getLogsPage(
        page: Int,
        limit: Int,
        statusIds: List<Int>,
        search: String? = null,
    ): Pair<List<DeliveryLog>, Boolean>
}
