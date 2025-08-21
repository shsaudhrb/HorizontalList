package com.ntg.lmd.utils

import com.ntg.lmd.R

enum class ValidationField { USERNAME, PASSWORD }

enum class ValidationError(
    val resId: Int,
) {
    USERNAME_EMPTY(R.string.error_username_empty),
    USERNAME_INVALID(R.string.error_username_invalid),
    PASSWORD_EMPTY(R.string.error_password_empty),
    PASSWORD_INVALID(R.string.error_password_invalid),
    AGENT_ID_INVALID(R.string.error_agent_id_invalid),
}

class ValidationViewModel(
    private val validator: Validator = Validator(),
) {
    class InputState {
        private var touched = false
        private var focused = false

        fun updateFocus(isFocused: Boolean) {
            if (focused && !isFocused) touched = true
            focused = isFocused
        }

        fun showError(hasError: Boolean): Boolean = touched && !focused && hasError
    }

    fun validateUsername(input: String): ValidationError? =
        when {
            input.isBlank() -> ValidationError.USERNAME_EMPTY
            !validator.isUsernameValid(input) -> ValidationError.AGENT_ID_INVALID
            else -> null
        }

    fun validatePassword(input: String): ValidationError? =
        when {
            input.isBlank() -> ValidationError.PASSWORD_EMPTY
            !validator.isPasswordValid(input) -> ValidationError.PASSWORD_INVALID
            else -> null
        }
}
