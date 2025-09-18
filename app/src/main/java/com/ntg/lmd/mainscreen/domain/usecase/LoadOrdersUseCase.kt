package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.data.model.Order
import com.ntg.lmd.mainscreen.domain.repository.LiveOrdersRepository

class LoadOrdersUseCase(
    private val repo: LiveOrdersRepository,
) {
    suspend operator fun invoke(pageSize: Int): Result<List<Order>> = repo.getAllLiveOrders(pageSize)
}
