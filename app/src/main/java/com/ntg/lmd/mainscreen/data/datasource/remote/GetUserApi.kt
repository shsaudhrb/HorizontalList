package com.ntg.lmd.mainscreen.data.datasource.remote

import com.ntg.lmd.mainscreen.data.model.ActiveUsersEnvelope
import retrofit2.http.GET

interface GetUsersApi {
    @GET("get-all-users")
    suspend fun getActiveUsers(): ActiveUsersEnvelope
}
