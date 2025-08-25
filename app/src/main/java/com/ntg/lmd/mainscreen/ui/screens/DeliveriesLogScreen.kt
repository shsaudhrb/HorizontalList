package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class DeliveryLog(
    val slaIcon: @Composable () -> Unit,
    val orderDate: String,
    val deliveryTime: String,
    val orderId: String
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun deliveriesLogScreen(navController: NavController) {
    val logs = listOf(
        DeliveryLog(
            slaIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = "Delivered") },
            orderDate = "15/9/2019 3:00 PM",
            deliveryTime = "20 minutes ago",
            orderId = "#2345645645643"
        ),
        DeliveryLog(
            slaIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = "Delivered") },
            orderDate = "16/9/2019 5:00 PM",
            deliveryTime = "25 minutes ago",
            orderId = "#2345645645214"
        ),
        DeliveryLog(
            slaIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = "Delivered") },
            orderDate = "17/9/2019 5:00 PM",
            deliveryTime = "30 minutes ago",
            orderId = "#53456445214"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SLA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Order Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Delivery Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Divider()

        // List of Logs
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            log.slaIcon()
                            Text(log.orderDate)
                            Text(log.deliveryTime)
                        }
                        Text(
                            text = log.orderId,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }
            }
        }
    }
}