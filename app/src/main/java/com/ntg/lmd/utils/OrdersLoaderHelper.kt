package com.ntg.lmd.utils

import com.ntg.lmd.mainscreen.ui.screens.orders.model.OrderUI

object OrdersLoaderHelper {
    fun loadFromAssets(context: android.content.Context): List<OrderUI> {
        val json =
            context.assets
                .open("order.json")
                .bufferedReader()
                .use { it.readText() }
        val arr = org.json.JSONArray(json)
        val out = ArrayList<OrderUI>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out +=
                OrderUI(
                    id = o.getInt("id").toLong(),
                    orderNumber = o.getString("orderNumber"),
                    status = o.getString("status"),
                    customerName = o.getString("customerName"),
                    totalPrice = o.getDouble("total"),
                    distanceMeters =
                        o
                            .optDouble("distanceMeters", Double.NaN)
                            .let { if (it.isNaN()) null else it },
                    details = null, // JSON doesn’t have 'details'
                )
        }
        return out
    }
}
