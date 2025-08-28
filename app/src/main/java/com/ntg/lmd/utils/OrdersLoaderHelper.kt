package com.ntg.lmd.utils

/*
import com.ntg.lmd.mainscreen.domain.model.OrderInfo


object OrdersLoaderHelper {
    fun loadFromAssets(context: android.content.Context): List<OrderInfo> {
        val json =
            context.assets
                .open("order.json")
                .bufferedReader()
                .use { it.readText() }
        val arr = org.json.JSONArray(json)
        val out = ArrayList<OrderInfo>(arr.length())

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val phone =
                (
                    if (o.has("customerPhone")) {
                        o.optString("customerPhone")
                    } else if (o.has("phone")) {
                        o.optString("phone")
                    } else {
                        o.optString("customer_phone", "") // fallback snake_case
                    }
                ).trim().takeIf { it.isNotEmpty() }
            out +=
                OrderInfo(
                    id = o.getInt("id").toString(),
                    orderNumber = o.getString("orderNumber"),
                    status = o.getString("status"),
                    customerName = o.getString("customerName"),
                    customerPhone = phone,
                    totalPrice = "---",
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
*/
