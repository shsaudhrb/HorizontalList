package com.ntg.lmd.authentication.ui.model

import com.ntg.lmd.utils.ValidationError

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @androidx.annotation.StringRes val message: Int? = null,
    val isFormValid: Boolean = false,
    val usernameError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val showUsernameError: Boolean = false,
    val showPasswordError: Boolean = false,
    val loginSuccess: Boolean = false,
)
