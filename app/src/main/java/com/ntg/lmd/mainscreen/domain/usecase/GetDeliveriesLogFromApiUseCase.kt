package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository

object DeliveryStatusIds {
    const val CANCELLED = 3
    const val FAILED = 7
    const val DELIVERED = 8

    val DEFAULT_LOG_STATUSES: List<Int> = listOf(CANCELLED, FAILED, DELIVERED)
}

class GetDeliveriesLogFromApiUseCase(
    private val repo: DeliveriesLogRepository,
) {
    suspend operator fun invoke(
        page: Int,
        limit: Int,
        statusIds: List<Int> = DeliveryStatusIds.DEFAULT_LOG_STATUSES,
        search: String? = null,
    ): Pair<List<DeliveryLog>, Boolean> = repo.getLogsPage(page, limit, statusIds, search)
}
