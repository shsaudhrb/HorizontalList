package com.ntg.lmd.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.ntg.lmd.mainscreen.domain.model.SearchController

// ---------- Public Top Bar Config ----------
data class TopBarConfigWithTitle(
    val title: String,
    val search: SearchController,
    val showSearchIcon: Boolean,
    val actionButtonLabel: String? = null,
    val onActionButtonClick: (() -> Unit)? = null,
    val actionIcon: ImageVector? = null,
    val onActionIconClick: (() -> Unit)? = null,
    val searchPlaceholder: String? = null,
    val searchActionIcon: ImageVector? = null,
    val onSearchIconClick: (() -> Unit)? = null,
)
