package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class WebSocketMessage(
    @SerializedName("type")
    val type: String?,
    @SerializedName("data")
    val data: Any?,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
