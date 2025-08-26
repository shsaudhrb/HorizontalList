package com.ntg.lmd.authentication.data.datasource.model

import com.google.gson.annotations.SerializedName

data class RefreshTokenResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: LoginRefreshToken?,
)

data class LoginRefreshToken(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
    @SerializedName("refresh_expires_at")
    val refreshExpiresAt: String?,
)
