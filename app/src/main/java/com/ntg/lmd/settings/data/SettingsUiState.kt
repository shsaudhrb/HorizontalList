package com.ntg.lmd.settings.data

import com.ntg.lmd.settings.ui.viewmodel.AppLanguage
import com.ntg.lmd.settings.ui.viewmodel.NotificationWindow

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.EN,
    val window: NotificationWindow = NotificationWindow.D15,
)

sealed interface LogoutUiState {
    data object Idle : LogoutUiState

    data object Loading : LogoutUiState

    data object Success : LogoutUiState

    data class Error(
        val message: String,
    ) : LogoutUiState
}
