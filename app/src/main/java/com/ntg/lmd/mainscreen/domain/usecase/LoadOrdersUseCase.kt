package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.data.model.LiveOrdersResponse
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import kotlinx.coroutines.flow.Flow

class LoadOrdersUseCase(
    private val repo: OrdersRepository
) {
    suspend operator fun invoke(): Flow<Result<LiveOrdersResponse>> = repo.getLiveOrders()
}
