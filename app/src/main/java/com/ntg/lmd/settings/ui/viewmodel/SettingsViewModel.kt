package com.ntg.lmd.settings.ui.viewmodel

import android.content.Context
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
import java.io.IOException

private const val LANGUAGE_AR = "ar"
private const val LANGUAGE_EN = "en"

private const val DAYS_15 = 15
private const val DAYS_30 = 30
private const val DAYS_90 = 90

private const val HOURS_IN_DAY = 24L
private const val MINUTES_IN_HOUR = 60L
private const val SECONDS_IN_MINUTE = 60L
private const val MILLIS_IN_SECOND = 1000L

class SettingsViewModel(
    private val prefs: SettingsPreferenceDataSource,
    private val logoutManager: LogoutManager,
) : ViewModel() {
    private val _logoutState = MutableStateFlow<LogoutUiState>(LogoutUiState.Idle)
    val logoutState: StateFlow<LogoutUiState> = _logoutState

    private val _ui =
        MutableStateFlow(
            SettingsUiState(
                language = if (prefs.getLanguage() == LANGUAGE_AR) AppLanguage.AR else AppLanguage.EN,
                window =
                    when (prefs.getNotificationWindowDays()) {
                        DAYS_30 -> NotificationWindow.D30
                        DAYS_90 -> NotificationWindow.D90
                        else -> NotificationWindow.D15
                    },
            ),
        )
    val ui: StateFlow<SettingsUiState> = _ui

    fun setLanguage(lang: AppLanguage, context: Context) {
        val code = if (lang == AppLanguage.AR) "ar" else "en"
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putString("lang_code", code).apply()
    }

    fun setNotificationWindow(win: NotificationWindow) {
        _ui.update { it.copy(window = win) }
        prefs.setNotificationWindowDays(win.days)
    }

    fun computeSinceEpochMs(nowMs: Long = System.currentTimeMillis()): Long {
        val days = _ui.value.window.days
        return nowMs - days * HOURS_IN_DAY * MINUTES_IN_HOUR * SECONDS_IN_MINUTE * MILLIS_IN_SECOND
    }

    fun logout() {
        if (_logoutState.value is LogoutUiState.Loading) return

        viewModelScope.launch {
            _logoutState.value = LogoutUiState.Loading
            try {
                logoutManager.logout()
                _logoutState.value = LogoutUiState.Success
            } catch (e: IOException) {
                _logoutState.value = LogoutUiState.Error("Network error: ${e.message ?: "Logout failed"}")
            }
        }
    }

    fun resetLogoutState() {
        _logoutState.value = LogoutUiState.Idle
    }
}

enum class AppLanguage { EN, AR }

enum class NotificationWindow(
    val days: Int,
) {
    D15(DAYS_15),
    D30(DAYS_30),
    D90(DAYS_90),
}
