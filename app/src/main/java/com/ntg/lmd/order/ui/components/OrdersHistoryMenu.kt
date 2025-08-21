package com.ntg.lmd.order.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ntg.lmd.order.domain.model.OrderUi

@Composable
fun ordersHistoryMenu(
    orders: List<OrderUi>,
    onExportPdf: (List<OrderUi>) -> Unit
) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Export PDF") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = {
                    open = false
                    onExportPdf(orders)
                }
            )
        }
    }
}
