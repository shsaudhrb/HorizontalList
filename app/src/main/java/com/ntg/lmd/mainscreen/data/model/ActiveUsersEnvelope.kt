package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class ActiveUsersEnvelope(
    val success: Boolean,
    val data: List<ActiveUserDto>?,
    @SerializedName("total_count") val totalCount: Int?,
    @SerializedName("current_user_id") val currentUserId: String?
)

data class ActiveUserDto(
    val id: String,
    val name: String
)