package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ntg.lmd.ui.theme.SuccessGreen

sealed class OrderActions {
    data object Confirm : OrderActions()

    data object PickUp : OrderActions()

    data object Start : OrderActions()

    data object Deliver : OrderActions()

    data object Fail : OrderActions()
}

@Composable
fun statusTint(status: String) =
    if (status.equals("confirmed", ignoreCase = true) ||
        status.equals("added", ignoreCase = true)
    ) {
        SuccessGreen
    } else {
        MaterialTheme.colorScheme.onSurface
    }
