package com.ntg.lmd.authentication.ui.viewmodel.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.MyApp
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
    // Single StateFlow for all UI state
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // track field focus
    private val usernameState = ValidationViewModel.InputState()
    private val passwordState = ValidationViewModel.InputState()

    // updates the Username & Password fields
    fun updateUsername(email: String) {
        _uiState.update {
            it.copy(
                username = email,
                usernameError = null,
                showUsernameError = false,
            )
        }
        updateValidation(email, ValidationField.USERNAME)
    }

    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                showPasswordError = false,
            )
        }
        updateValidation(password, ValidationField.PASSWORD)
    }

    // updates focus tracking
    fun updateUsernameFocus(isFocused: Boolean) {
        usernameState.updateFocus(isFocused)
        if (!isFocused) {
            updateValidation(_uiState.value.username, ValidationField.USERNAME)
        }
        updateErrorVisibility(ValidationField.USERNAME)
    }

    fun updatePasswordFocus(isFocused: Boolean) {
        passwordState.updateFocus(isFocused)
        if (!isFocused) {
            updateValidation(_uiState.value.password, ValidationField.PASSWORD)
        }
        updateErrorVisibility(ValidationField.PASSWORD)
    }

    // update validation for username/password from validationViewModel
    private fun updateValidation(
        input: String,
        field: ValidationField,
    ) {
        val error =
            when (field) {
                ValidationField.USERNAME -> validationViewModel.validateUsername(input)
                ValidationField.PASSWORD -> validationViewModel.validatePassword(input)
            }

        _uiState.update { current ->
            when (field) {
                ValidationField.USERNAME -> current.copy(usernameError = error)
                ValidationField.PASSWORD -> current.copy(passwordError = error)
            }
        }

        updateFormValidation()
        updateErrorVisibility(field)
    }

    // show error if the field was touched and not focused
    private fun updateErrorVisibility(field: ValidationField) {
        _uiState.update {
            when (field) {
                ValidationField.USERNAME ->
                    it.copy(showUsernameError = usernameState.showError(it.usernameError != null))
                ValidationField.PASSWORD ->
                    it.copy(
                        showPasswordError = passwordState.showError(it.passwordError != null),
                    )
            }
        }
    }

    private fun updateFormValidation() {
        val current = _uiState.value
        val isValid =
            current.usernameError == null &&
                current.passwordError == null &&
                current.username.isNotBlank() &&
                current.password.isNotBlank()

        _uiState.update { it.copy(isFormValid = isValid) }
    }

    fun submit(onResult: (Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val username = _uiState.value.username.trim()
            val password = _uiState.value.password

            _uiState.update { it.copy(isLoading = true, message = null, loginSuccess = false) }
            val result = authRepo.login(username, password)

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            message = R.string.msg_welcome,
                        )
                    }
                    onResult(true)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = false,
                            message = null,
                            errorMessage = result.error.message ?: "Unknown error",
                        )
                    }
                    onResult(false)
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
}

class LoginViewModelFactory(
    private val app: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val myApp = app as MyApp
        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(myApp.authRepo) as T
    }
}