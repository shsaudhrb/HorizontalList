package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class RealtimeMessage(
    @SerializedName("ref")
    val ref: String?,
    @SerializedName("event")
    val event: String?,
    @SerializedName("payload")
    val payload: RealtimePayload?,
    @SerializedName("topic")
    val topic: String?,
)

data class RealtimePayload(
    @SerializedName("table")
    val table: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("record")
    val record: Order?,
    @SerializedName("columns")
    val columns: List<Column>?,
    @SerializedName("errors")
    val errors: Any?,
    @SerializedName("schema")
    val schema: String?,
    @SerializedName("commit_timestamp")
    val commitTimestamp: String?,
)
