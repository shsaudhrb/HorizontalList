package com.ntg.lmd.notification.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DeepLinkViewModel : ViewModel() {
    private val _openNotifications = MutableStateFlow(false)

    fun setFromIntent(intent: Intent?) {
        val d = intent?.data
        _openNotifications.value = (d?.scheme == "myapp" && d.host == "notifications")
    }

    /** Return current flag and clear it so it’s consumed once. */
    fun consumeOpenNotifications(): Boolean {
        val v = _openNotifications.value
        _openNotifications.value = false
        return v
    }
}
