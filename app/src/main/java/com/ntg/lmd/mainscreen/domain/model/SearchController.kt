package com.ntg.lmd.mainscreen.domain.model

data class SearchController(
    val searching: Boolean,
    val searchText: String,
    val onSearchingChange: (Boolean) -> Unit,
    val onSearchTextChange: (String) -> Unit,
    val onSubmit: (String) -> Unit = {},
)
