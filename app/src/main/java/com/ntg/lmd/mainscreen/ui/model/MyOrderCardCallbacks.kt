package com.ntg.lmd.mainscreen.ui.model

import com.ntg.lmd.mainscreen.ui.components.OrderActions

data class MyOrderCardCallbacks(
    val onDetails: () -> Unit,
    val onCall: () -> Unit,
    val onAction: (OrderActions) -> Unit,
    val onReassignRequested: () -> Unit,
)
