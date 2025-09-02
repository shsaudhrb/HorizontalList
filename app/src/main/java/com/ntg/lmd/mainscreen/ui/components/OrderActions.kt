package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ntg.lmd.ui.theme.SuccessGreen

sealed class ActionDialog {
    data object Confirm : ActionDialog()

    data object PickUp : ActionDialog()

    data object Start : ActionDialog()

    data object Deliver : ActionDialog()

    data object Fail : ActionDialog()
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
