package com.ntg.lmd.settings.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.MyApp
import com.ntg.lmd.network.authheader.SecureTokenStore
import com.ntg.lmd.network.sockets.SocketIntegration
import com.ntg.lmd.settings.data.SettingsPreferenceDataSource
import com.ntg.lmd.utils.LogoutManager

class SettingsViewModelFactory(
    private val app: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown VM class $modelClass"
        }
        val prefs = SettingsPreferenceDataSource(app)
        val myApp = app as MyApp
        val tokenStore = SecureTokenStore(app)
        val socket: SocketIntegration? = myApp.socket

        val logoutManager =
            LogoutManager(
                tokenStore = tokenStore,
                socket = socket,
            )

        return SettingsViewModel(
            prefs = prefs,
            logoutManager = logoutManager,
        ) as T
    }
}
