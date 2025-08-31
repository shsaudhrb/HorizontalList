package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.DeliveryState
import com.ntg.lmd.ui.theme.SuccessGreen

private val TIME_CELL = 100.dp

@Composable
fun timeCell(log: DeliveryLog) {
    val color =
        when (log.state) {
            DeliveryState.DELIVERED -> SuccessGreen
            DeliveryState.CANCELLED, DeliveryState.FAILED -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Box(Modifier.width(TIME_CELL), contentAlignment = Alignment.Center) {
        Text(log.deliveryTime, color = color, textAlign = TextAlign.Center, maxLines = 1)
    }
}
