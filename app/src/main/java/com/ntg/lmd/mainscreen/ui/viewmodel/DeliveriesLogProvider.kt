package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import com.ntg.lmd.mainscreen.data.datasource.remote.OrdersApi
import com.ntg.lmd.mainscreen.data.repository.DeliveriesLogRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.DeliveriesLogRepository
import com.ntg.lmd.mainscreen.domain.usecase.GetDeliveriesLogFromApiUseCase
import com.ntg.lmd.network.core.RetrofitProvider

object DeliveriesLogProvider {
    private fun repository(
        @Suppress("UNUSED_PARAMETER") context: Context,
    ): DeliveriesLogRepository = DeliveriesLogRepositoryImpl(ordersApi())

    fun getLogsUseCase(context: Context): GetDeliveriesLogFromApiUseCase =
        GetDeliveriesLogFromApiUseCase(
            repository(context),
        )

    private fun ordersApi(): OrdersApi = RetrofitProvider.ordersApi
}
