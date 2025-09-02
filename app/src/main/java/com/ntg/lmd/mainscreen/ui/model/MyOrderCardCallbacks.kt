package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.ui.components.ActionDialog

data class MyOrderCardCallbacks(
    val onDetails: () -> Unit,
    val onCall: () -> Unit,
    val onAction: (ActionDialog) -> Unit,
    val onReassignRequested: () -> Unit,
)
