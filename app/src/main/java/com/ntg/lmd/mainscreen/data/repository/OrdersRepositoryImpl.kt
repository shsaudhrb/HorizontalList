package com.ntg.lmd.mainscreen.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.ntg.lmd.mainscreen.domain.model.OrderInfo
import com.ntg.lmd.mainscreen.domain.repository.OrdersRepository
import java.io.IOException

class OrdersRepositoryImpl(
    private val gson: Gson = Gson()
) : OrdersRepository {

    override suspend fun loadOrdersFromAssets(
        context: Context,
        assetFile: String
    ): List<OrderInfo> {
        return try {
            val json = context.assets.open(assetFile).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<OrderInfo>>() {}.type
            gson.fromJson<List<OrderInfo>>(json, type) ?: emptyList()
        } catch (e: IOException) {
            Log.e("OrdersRepositoryImpl", "Failed to read $assetFile", e)
            emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e("OrdersRepositoryImpl", "Invalid JSON format in $assetFile", e)
            emptyList()
        }
    }
}