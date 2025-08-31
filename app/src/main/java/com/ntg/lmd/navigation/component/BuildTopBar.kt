package com.ntg.lmd.navigation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.RouteUiSpec
import com.ntg.lmd.navigation.TopBarConfigWithTitle

@Composable
fun buildTopBar(
    spec: RouteUiSpec,
    search: SearchController,
): TopBarConfigWithTitle =
    TopBarConfigWithTitle(
        title = spec.title,
        search = search,
        showSearchIcon = spec.showSearchIcon,
        actionButtonLabel = spec.actionButtonLabel,
        onActionButtonClick = spec.onActionButtonClick,
        actionIcon = spec.actionIcon,
        onActionIconClick = spec.onActionIconClick,
        searchPlaceholder = spec.searchPlaceholder,
        searchActionIcon = if (spec.showSearchIcon) Icons.Filled.Search else null,
        onSearchIconClick = { search.onToggle(true) },
    )
