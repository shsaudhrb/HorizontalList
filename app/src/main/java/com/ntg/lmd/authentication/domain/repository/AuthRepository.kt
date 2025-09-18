package com.ntg.lmd.authentication.domain.repository

import com.ntg.lmd.authentication.data.datasource.model.User
import com.ntg.lmd.network.queue.NetworkResult

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): NetworkResult<Unit>

    fun getCurrentUser(): User?

    fun isAuthenticated(): Boolean

    val lastLoginName: String?
}
