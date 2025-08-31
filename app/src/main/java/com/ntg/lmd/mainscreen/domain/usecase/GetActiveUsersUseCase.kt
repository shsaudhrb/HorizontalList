package com.ntg.lmd.mainscreen.domain.usecase

import com.ntg.lmd.mainscreen.domain.model.ActiveUser
import com.ntg.lmd.mainscreen.domain.repository.UsersRepository

class GetActiveUsersUseCase(private val repo: UsersRepository) {
    suspend operator fun invoke(): Pair<List<ActiveUser>, String?> = repo.getActiveUsers()
}