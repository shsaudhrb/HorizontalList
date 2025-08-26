package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class LiveOrdersData(
    @SerializedName("initial_orders")
    val initialOrders: List<Order> = emptyList(),

    @SerializedName("realtime_config")
    val realtimeConfig: RealtimeConfig? = null,

    @SerializedName("subscription_id")
    val subscriptionId: String? = null,

    @SerializedName("channel_name")
    val channelName: String? = null
)