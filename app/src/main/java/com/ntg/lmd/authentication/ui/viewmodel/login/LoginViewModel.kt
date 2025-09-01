package com.ntg.lmd.authentication.ui.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.R
import com.ntg.lmd.authentication.data.repositoryImp.AuthRepositoryImp
import com.ntg.lmd.authentication.ui.model.LoginUiState
import com.ntg.lmd.network.queue.NetworkResult
import com.ntg.lmd.utils.ValidationField
import com.ntg.lmd.utils.ValidationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepo: AuthRepositoryImp,
    private val validationViewModel: ValidationViewModel = ValidationViewModel(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val usernameState = ValidationViewModel.InputState()
    private val passwordState = ValidationViewModel.InputState()


    fun updateUsername(value: String) {
        _uiState.updateField(value, ValidationField.USERNAME)
        _uiState.applyValidation(validationViewModel, ValidationField.USERNAME, value)
        _uiState.updateFormValidation()
        _uiState.updateErrorVisibility(usernameState, passwordState, ValidationField.USERNAME)
    }

    fun updatePassword(value: String) {
        _uiState.updateField(value, ValidationField.PASSWORD)
        _uiState.applyValidation(validationViewModel, ValidationField.PASSWORD, value)
        _uiState.updateFormValidation()
        _uiState.updateErrorVisibility(usernameState, passwordState, ValidationField.PASSWORD)
    }

    fun updateUsernameFocus(isFocused: Boolean) {
        usernameState.updateFocus(isFocused)
        if (!isFocused) {
            val v = _uiState.value.username
            _uiState.applyValidation(validationViewModel, ValidationField.USERNAME, v)
            _uiState.updateFormValidation()
        }
        _uiState.updateErrorVisibility(usernameState, passwordState, ValidationField.USERNAME)
    }

    fun updatePasswordFocus(isFocused: Boolean) {
        passwordState.updateFocus(isFocused)
        if (!isFocused) {
            val v = _uiState.value.password
            _uiState.applyValidation(validationViewModel, ValidationField.PASSWORD, v)
            _uiState.updateFormValidation()
        }
        _uiState.updateErrorVisibility(usernameState, passwordState, ValidationField.PASSWORD)
    }

    fun submit(onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val username = _uiState.value.username.trim()
        val password = _uiState.value.password

        _uiState.setLoading()
        when (val result = authRepo.login(username, password)) {
            is NetworkResult.Success -> {
                // If you have a display name from repo, use it; else fallback to username
                val displayName = try { authRepo.lastLoginName } catch (_: Throwable) { null } ?: username
                _uiState.handleSuccess(displayName)
                onResult(true)
            }
            is NetworkResult.Error -> {
                _uiState.handleError(result.error.message)
                onResult(false)
            }
            is NetworkResult.Loading -> _uiState.setLoading()
        }
    }
}

// ---------- File-level private helpers (pure: only mutate the provided state flow) ----------

private fun MutableStateFlow<LoginUiState>.updateField(input: String, field: ValidationField) {
    update {
        when (field) {
            ValidationField.USERNAME -> it.copy(username = input, usernameError = null, showUsernameError = false)
            ValidationField.PASSWORD -> it.copy(password = input, passwordError = null, showPasswordError = false)
        }
    }
}

private fun MutableStateFlow<LoginUiState>.applyValidation(
    validator: ValidationViewModel,
    field: ValidationField,
    input: String
) {
    val error = when (field) {
        ValidationField.USERNAME -> validator.validateUsername(input)
        ValidationField.PASSWORD -> validator.validatePassword(input)
    }
    update {
        when (field) {
            ValidationField.USERNAME -> it.copy(usernameError = error)
            ValidationField.PASSWORD -> it.copy(passwordError = error)
        }
    }
}

private fun MutableStateFlow<LoginUiState>.updateErrorVisibility(
    usernameState: ValidationViewModel.InputState,
    passwordState: ValidationViewModel.InputState,
    field: ValidationField
) {
    update {
        when (field) {
            ValidationField.USERNAME ->
                it.copy(showUsernameError = usernameState.showError(it.usernameError != null))
            ValidationField.PASSWORD ->
                it.copy(showPasswordError = passwordState.showError(it.passwordError != null))
        }
    }
}

private fun MutableStateFlow<LoginUiState>.updateFormValidation() {
    val s = value
    val isValid = s.usernameError == null &&
            s.passwordError == null &&
            s.username.isNotBlank() &&
            s.password.isNotBlank()
    update { it.copy(isFormValid = isValid) }
}

private fun MutableStateFlow<LoginUiState>.setLoading() {
    update { it.copy(isLoading = true, message = null, loginSuccess = false) }
}

private fun MutableStateFlow<LoginUiState>.handleSuccess(displayName: String?) {
    update {
        it.copy(
            isLoading = false,
            loginSuccess = true,
            message = R.string.msg_welcome,
            errorMessage = null,
            displayName = displayName
        )
    }
}

private fun MutableStateFlow<LoginUiState>.handleError(message: String?) {
    update {
        it.copy(
            isLoading = false,
            loginSuccess = false,
            message = null,
            errorMessage = message ?: "Unknown error",
        )
    }
}
