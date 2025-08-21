package com.ntg.lmd.navigation.component

import androidx.compose.runtime.Stable

@Stable
data class AppScaffoldConfig(
    val currentRoute: String,
    val title: String,
    val userName: String = "",
    val showOrdersMenu: Boolean = false,
)

@Stable
data class AppScaffoldActions(
    val onNavigate: (route: String) -> Unit,
    val onLogout: () -> Unit,
    val onOrdersMenuClick: (() -> Unit)? = null,
)