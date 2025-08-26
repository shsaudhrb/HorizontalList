package com.ntg.lmd.notification.ui.model

sealed interface NotificationsState {
    data object Loading : NotificationsState

    data class Error(
        val message: String,
    ) : NotificationsState

    data object Empty : NotificationsState

    data class Success(
        val items: List<NotificationUi>,
    ) : NotificationsState
}
