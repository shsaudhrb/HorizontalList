package com.ntg.lmd.navigation

import androidx.compose.ui.graphics.vector.ImageVector

// ---------- Route UI spec that the Nav layer controls ----------
data class RouteUiSpec(
    val title: String,
    val showSearchIcon: Boolean,
    val searchPlaceholder: String? = null,
    val actionButtonLabel: String? = null,
    val onActionButtonClick: (() -> Unit)? = null,
    val actionIcon: ImageVector? = null,
    val onActionIconClick: (() -> Unit)? = null,
)