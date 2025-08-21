package com.ntg.lmd.notification.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.notification.data.remote.ServiceLocator
import com.ntg.lmd.notification.domain.usecase.ObserveNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.RefreshNotificationsUseCase

class NotificationsVMFactory(
    private val observe: ObserveNotificationsUseCase = ServiceLocator.observeNotificationsUseCase,
    private val refresh: RefreshNotificationsUseCase = ServiceLocator.refreshNotificationsUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NotificationsViewModel(
            observeNotifications = observe,
            refreshNotifications = refresh,
        ) as T
    }
}
