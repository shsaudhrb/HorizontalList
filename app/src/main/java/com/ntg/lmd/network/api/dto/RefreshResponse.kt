package com.ntg.lmd.network.api.dto

data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?,
)
