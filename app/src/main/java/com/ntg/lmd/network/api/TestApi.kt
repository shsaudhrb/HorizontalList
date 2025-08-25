package com.ntg.lmd.network.api

import com.ntg.lmd.network.api.dto.LoginRequest
import com.ntg.lmd.network.api.dto.LoginResponse
import com.ntg.lmd.network.api.dto.RefreshTokenRequest
import com.ntg.lmd.network.api.dto.RefreshTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TestApi {
    @POST("login")
    suspend fun login(
        @Body req: LoginRequest,
    ): LoginResponse

    @POST("refresh-token")
    suspend fun refreshToken(
        @Body req: RefreshTokenRequest,
    ): RefreshTokenResponse
}
