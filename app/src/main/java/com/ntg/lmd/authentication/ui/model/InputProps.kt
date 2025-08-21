package com.ntg.lmd.authentication.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable

@Stable
data class InputProps(
    val value: String,
    val onValueChange: (String) -> Unit,
    @StringRes val label: Int,
    @StringRes val placeholder: Int,
    @StringRes val errorResId: Int? = null,
    val showError: Boolean = false,
    val onFocusChange: (Boolean) -> Unit = {},
)
