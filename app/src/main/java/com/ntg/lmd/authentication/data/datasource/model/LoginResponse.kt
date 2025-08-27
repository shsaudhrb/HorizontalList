package com.ntg.lmd.authentication.data.datasource.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: LoginRefreshToken?,
    @SerializedName("error")
    val error: String?,
)
