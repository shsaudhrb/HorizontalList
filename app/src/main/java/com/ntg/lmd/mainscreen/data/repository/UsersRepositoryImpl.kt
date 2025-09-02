package com.ntg.lmd.mainscreen.data.repository

import com.ntg.lmd.mainscreen.data.datasource.remote.GetUsersApi
import com.ntg.lmd.mainscreen.domain.model.ActiveUser
import com.ntg.lmd.mainscreen.domain.model.toDomain
import com.ntg.lmd.mainscreen.domain.repository.UsersRepository

class UsersRepositoryImpl(
    private val api: GetUsersApi,
) : UsersRepository {
    override suspend fun getActiveUsers(): Pair<List<ActiveUser>, String?> {
        val env = api.getActiveUsers()
        if (!env.success) error("Failed to load users")
        val list = env.data?.map { it.toDomain() } ?: emptyList()
        return list to env.currentUserId
    }
}
