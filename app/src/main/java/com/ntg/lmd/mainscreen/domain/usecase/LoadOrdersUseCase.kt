package com.ntg.lmd.mainscreen.domain.usecase

import android.content.Context
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository

class LoadOrdersUseCase(
    private val repo: OrdersRepository
) {
    suspend operator fun invoke(
        context: Context,
        assetFile: String = "orders.json"
    ): List<OrderInfo> {
        return repo.loadOrdersFromAssets(context, assetFile)
    }
}
