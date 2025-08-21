package com.ntg.lmd.navigation

import androidx.compose.runtime.MutableState

// Search-related
data class AppSearchConfig(
    val enabled: Boolean = false,
    val searchingState: MutableState<Boolean>,
    val searchTextState: MutableState<String>,
    val onSearchSubmit: (String) -> Unit,
    val onSearchClick: (() -> Unit)? = null,
)
