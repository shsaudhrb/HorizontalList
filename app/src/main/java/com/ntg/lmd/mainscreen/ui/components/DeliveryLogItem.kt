package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog

private val ICON_CELL = 48.dp
private val TIME_CELL = 100.dp
private val GAP = 8.dp

@Composable
fun deliveryLogItem(log: DeliveryLog) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            val detailsWidth = maxWidth - ICON_CELL - TIME_CELL - (GAP * 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                statusCell(log.state)
                Spacer(Modifier.width(GAP))
                detailsCell(log, Modifier.width(detailsWidth))
                Spacer(Modifier.width(GAP))
                timeCell(log)
            }
        }
    }
}
