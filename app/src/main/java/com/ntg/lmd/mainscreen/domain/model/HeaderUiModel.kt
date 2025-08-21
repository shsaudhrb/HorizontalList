package com.ntg.lmd.mainscreen.domain.model

data class HeaderUiModel(
    val title: String,
    val showStartIcon: Boolean = true,
    val onStartClick: (() -> Unit)? = null,
    val showEndIcon: Boolean = true,
    val onEndIconClick: (() -> Unit)? = null,
)
