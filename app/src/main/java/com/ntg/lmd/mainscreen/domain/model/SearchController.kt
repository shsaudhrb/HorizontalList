package com.ntg.lmd.mainscreen.domain.model

import androidx.compose.runtime.MutableState

data class SearchController(
    val searching: MutableState<Boolean>,
    val text: MutableState<String>,
    val onSubmit: (String) -> Unit,
    val onToggle: (Boolean) -> Unit,
    val onTextChange: (String) -> Unit,
)
