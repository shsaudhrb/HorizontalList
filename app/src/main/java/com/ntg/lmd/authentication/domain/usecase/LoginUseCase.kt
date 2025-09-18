package com.ntg.lmd.authentication.domain.usecase

import com.ntg.lmd.authentication.data.datasource.model.User
import com.ntg.lmd.authentication.domain.repository.AuthRepository
import com.ntg.lmd.network.queue.NetworkError
import com.ntg.lmd.network.queue.NetworkResult

class LoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend fun execute(
        email: String,
        password: String,
    ): NetworkResult<User> =
        when (val result = authRepository.login(email, password)) {
            is NetworkResult.Success -> {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    NetworkResult.Success(user)
                } else {
                    NetworkResult.Error(
                        NetworkError.BadRequest("Failed to retrieve user after login"),
                    )
                }
            }
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> result
        }

    fun getLastLoginName(): String? = authRepository.lastLoginName
}
