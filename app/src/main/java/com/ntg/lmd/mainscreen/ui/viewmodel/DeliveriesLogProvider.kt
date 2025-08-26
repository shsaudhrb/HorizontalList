package com.ntg.lmd.mainscreen.ui.viewmodel

import android.content.Context
import com.google.gson.Gson
import com.ntg.lmd.mainscreen.data.repository.OrderLogRepositoryImpl
import com.ntg.lmd.mainscreen.domain.repository.OrderLogRepository
import com.ntg.lmd.mainscreen.domain.usecase.LoadDeliveryLogsUseCase

object DeliveriesLogProvider {
    private val gson: Gson by lazy { Gson() }

    private fun repository(context: Context): OrderLogRepository {
        val appCtx = context.applicationContext
        return OrderLogRepositoryImpl(appCtx, gson)
    }

    fun loadDeliveryLogsUseCase(context: Context): LoadDeliveryLogsUseCase {
        val repo = repository(context)
        return LoadDeliveryLogsUseCase(repo)
    }
}
