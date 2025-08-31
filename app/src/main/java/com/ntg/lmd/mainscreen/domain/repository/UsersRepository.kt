package com.ntg.lmd.mainscreen.domain.repository

import com.ntg.lmd.mainscreen.domain.model.ActiveUser

interface UsersRepository {
    suspend fun getActiveUsers(): Pair<List<ActiveUser>, String?>
}