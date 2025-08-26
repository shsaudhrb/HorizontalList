package com.ntg.lmd.mainscreen.domain.repository

import android.content.Context
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

interface OrdersRepository {

    // load orders from local assets (orders.json)
    suspend fun loadOrdersFromAssets(context: Context, assetFile: String = "orders.json"): List<OrderInfo>
}