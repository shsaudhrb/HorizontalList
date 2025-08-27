package com.ntg.lmd.settings.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.utils.LogoutManager

class SettingsViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val prefs = SettingsPreferenceDataSource(appContext)
            val tokenStore = SecureTokenStore(appContext)
            val logoutManager =
                LogoutManager(
                    tokenStore = tokenStore,
                )
            return SettingsViewModel(prefs, logoutManager) as T
        }
        error("Unknown VM: ${modelClass.name}")
    }
}
