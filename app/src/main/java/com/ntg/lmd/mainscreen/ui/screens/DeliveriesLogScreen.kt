package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ntg.lmd.ui.theme.SuccessGreen

data class DeliveryLog(
    val orderDate: String,
    val deliveryTime: String,
    val orderId: String
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun deliveriesLogScreen(navController: NavController) {
    val logs = listOf(
        DeliveryLog("15/9/2019 3:00 PM", "20 mins ago", "#2345645645643"),
        DeliveryLog("16/9/2019 5:00 PM", "25 mins ago", "#2345645645214"),
        DeliveryLog("17/9/2019 5:00 PM", "30 mins ago", "#53456445214")
    )

    val iconWidth = 60.dp
    val detailsWidth = 220.dp
    val timeWidth = 120.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(iconWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SLA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                "Order Details",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(detailsWidth),
                textAlign = TextAlign.Center
            )
            Text(
                "Delivery Time",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(timeWidth),
                textAlign = TextAlign.Center
            )
        }

        Divider()

        // Logs
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // SLA icon
                        Box(
                            modifier = Modifier.width(iconWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Delivered",
                                tint = SuccessGreen
                            )
                        }

                        // Order details: date + order ID stacked vertically
                        Column(
                            modifier = Modifier.width(detailsWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = log.orderDate,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = log.orderId,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Delivery time
                        Text(
                            text = log.deliveryTime,
                            color = SuccessGreen,
                            modifier = Modifier.width(timeWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}