package com.ntg.lmd.notification.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ntg.lmd.notification.data.model.FCMServiceLocator
import com.ntg.lmd.notification.domain.usecase.ObserveNotificationsUseCase
import com.ntg.lmd.notification.domain.usecase.RefreshNotificationsUseCase

class NotificationsVMFactory(
    private val observe: ObserveNotificationsUseCase = FCMServiceLocator.observeNotificationsUseCase,
    private val refresh: RefreshNotificationsUseCase = FCMServiceLocator.refreshNotificationsUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationsViewModel(
            observeNotifications = observe,
            refreshNotifications = refresh,
        ) as T
}
