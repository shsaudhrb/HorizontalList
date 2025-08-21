package com.ntg.lmd.order.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.ntg.lmd.order.ui.components.ordersHistoryMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.order.domain.model.OrderUi
import com.ntg.lmd.order.ui.components.exportOrdersPdf
import com.ntg.lmd.order.ui.components.sharePdf
import com.ntg.lmd.order.ui.components.timeHelper
import com.ntg.lmd.order.ui.viewmodel.OrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ordersHistoryContent(
    orders: List<OrderUi>,
    currency: String = "L.E",
) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(orders, key = { it.number }) { order ->
                OrderCard(order = order, currency = currency)
            }
        }
}


@SuppressLint("DefaultLocale")
@Composable
private fun OrderCard(order: OrderUi, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.number,color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${order.total}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(currency,color =Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(order.customer, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeHelper(order.createdAtMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun OrdersHistoryRoute(navController: NavHostController) {
    val vm: OrdersViewModel = viewModel()
    val orders by vm.orders.collectAsState(emptyList())
    ordersHistoryContent(orders)
}


