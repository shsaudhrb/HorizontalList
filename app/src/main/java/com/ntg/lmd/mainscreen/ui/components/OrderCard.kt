package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.OrderInfo

@Composable
fun orderCard(
    order: OrderInfo,
    onAddClick: (OrderInfo) -> Unit,
    onOrderClick: (OrderInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onOrderClick(order) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.size(width = 270.dp, height = 150.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            orderHeader(order)
            orderItemsCount(order.itemsCount)
            Spacer(Modifier.weight(1f))
            orderAddButton(order, onAddClick)
        }
    }
}
