package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class RealtimeConfig(
    @SerializedName("channel_name") val channelName: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    val filters: Filters? = null,
    @SerializedName("user_orders_only") val userOrdersOnly: Boolean? = null,
    @SerializedName("subscription_config") val subscriptionConfig: SubscriptionConfig? = null
)