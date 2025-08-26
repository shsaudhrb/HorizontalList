package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain
import com.ntg.lmd.mainscreen.domain.repository.OrderLogRepository

class LoadDeliveryLogsUseCase(
    private val repository: OrderLogRepository,
) {
    suspend operator fun invoke(): List<DeliveryLogDomain> =
        // sort by newest first
        repository.loadHistory().sortedByDescending { it.createdAtMillis }
}
