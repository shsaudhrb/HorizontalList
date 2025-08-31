package com.ntg.lmd.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.settings.data.LogoutUiState
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.settings.data.SettingsUiState
import com.ntg.lmd.utils.LogoutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private const val LANGUAGE_AR = "ar"
private const val LANGUAGE_EN = "en"

private const val DAYS_15 = 15
private const val DAYS_30 = 30
private const val DAYS_90 = 90

class SettingsViewModel(
    private val prefs: SettingsPreferenceDataSource,
    private val logoutManager: LogoutManager,
) : ViewModel() {

    private val _logoutState = MutableStateFlow<LogoutUiState>(LogoutUiState.Idle)
    val logoutState: StateFlow<LogoutUiState> = _logoutState

    private val _ui = MutableStateFlow(
        SettingsUiState(
            language = if (prefs.getLanguage() == LANGUAGE_AR) AppLanguage.AR else AppLanguage.EN,
            window = when (prefs.getNotificationWindowDays()) {
                DAYS_30 -> NotificationWindow.D30
                DAYS_90 -> NotificationWindow.D90
                else -> NotificationWindow.D15
            },
        )
    )
    val ui: StateFlow<SettingsUiState> = _ui

    fun setLanguage(lang: AppLanguage) {
        val code = if (lang == AppLanguage.AR) LANGUAGE_AR else LANGUAGE_EN
        prefs.setLanguage(code)
        _ui.update { it.copy(language = lang) }
    }

    fun setNotificationWindow(win: NotificationWindow) {
        _ui.update { it.copy(window = win) }
        prefs.setNotificationWindowDays(win.days)
    }


    fun logout() {
        if (_logoutState.value is LogoutUiState.Loading) return
        viewModelScope.launch {
            _logoutState.value = LogoutUiState.Loading
            try {
                logoutManager.logout()
                _logoutState.value = LogoutUiState.Success
            } catch (e: IOException) {
                _logoutState.value =
                    LogoutUiState.Error("Network error: ${e.message ?: "Logout failed"}")
            } catch (e: HttpException) {
                _logoutState.value =
                    LogoutUiState.Error("Server error (${e.code()}): ${e.message() ?: "Logout failed"}")
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.value = LogoutUiState.Idle
    }
}

enum class AppLanguage { EN, AR }
enum class NotificationWindow(val days: Int) { D15(DAYS_15), D30(DAYS_30), D90(DAYS_90) }