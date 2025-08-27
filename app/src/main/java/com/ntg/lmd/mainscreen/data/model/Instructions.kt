package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class Instructions(
    @SerializedName("connect_to_channel")
    val connectToChannel: String?,
    @SerializedName("listen_for_events")
    val listenForEvents: String?,
    @SerializedName("broadcast_events")
    val broadcastEvents: String?,
)
