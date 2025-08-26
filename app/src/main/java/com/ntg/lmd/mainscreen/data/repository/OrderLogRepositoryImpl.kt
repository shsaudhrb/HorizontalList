package com.ntg.lmd.mainscreen.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ntg.lmd.mainscreen.data.mapper.toDomain
import com.ntg.lmd.mainscreen.data.model.OrderRecord
import com.ntg.lmd.mainscreen.domain.model.DeliveryLogDomain
import com.ntg.lmd.mainscreen.domain.repository.OrderLogRepository

class OrderHistoryRepositoryImpl(
    private val appContext: Context,
    private val gson: Gson
) : OrderLogRepository {

    override suspend fun loadHistory(): List<DeliveryLogDomain> {
        // load json file from assets
        val json = appContext.assets.open("OrderHistory.json")
            .bufferedReader().use { it.readText() }

        // list of order record
        val type = object : TypeToken<List<OrderRecord>>() {}.type
        val raw = gson.fromJson<List<OrderRecord>>(json, type) ?: emptyList()

       // map raw records into domain objects
        return raw.map { it.toDomain() }
    }
}