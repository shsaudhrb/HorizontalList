package com.ntg.lmd.mainscreen.data.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
    @SerializedName("id")
    val id: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("full_name")
    val fullName: String?
)