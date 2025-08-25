package com.ntg.lmd.network.api.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: LoginData?,
)

data class LoginData(
    @SerializedName("user")
    val user: User?,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
    @SerializedName("refresh_expires_at")
    val refreshExpiresAt: String?,
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
)

data class RefreshTokenResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: RefreshTokenData?,
)

data class RefreshTokenData(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
    @SerializedName("refresh_expires_at")
    val refreshExpiresAt: String?,
)

data class User(
    @SerializedName("id")
    val id: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("full_name")
    val fullName: String?,
)
