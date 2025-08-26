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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import com.ntg.lmd.mainscreen.domain.model.DeliveryState
import com.ntg.lmd.mainscreen.ui.viewmodel.DeliveriesLogViewModel
import com.ntg.lmd.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@Suppress("UNUSED_PARAMETER")
fun deliveriesLogScreen(
    navController: NavController,
    vm: DeliveriesLogViewModel = viewModel()
) {

    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.load(context) }

    val backStackEntry = navController.currentBackStackEntry

    val logs by vm.logs.collectAsState()

    val iconWidth = 60.dp
    val detailsWidth = 220.dp
    val timeWidth = 120.dp

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        val searchingFlow = h.getStateFlow("searching", false)
        val textFlow = h.getStateFlow("search_text", "")
        combine(
            searchingFlow,
            textFlow
        ) { enabled, text -> if (enabled) text else "" } // when search is closed, reset
            .distinctUntilChanged()
            .collect { query -> vm.searchById(query) }
    }

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("searching", false).collect { enabled ->
            if (!enabled) vm.searchById("") // restore full list
        }
    }

    LaunchedEffect(backStackEntry) {
        val h = backStackEntry?.savedStateHandle ?: return@LaunchedEffect
        h.getStateFlow("search_submit", "").collect { submitted ->
            if (submitted.isNotEmpty()) vm.searchById(submitted)
        }
    }

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
                    stringResource(R.string.SLA),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                stringResource(R.string.order_details),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(detailsWidth),
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.delivery_time),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.width(timeWidth),
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider()

        // Logs
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log: DeliveryLog ->
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
                            when (log.state) {
                                DeliveryState.DELIVERED -> Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Delivered",
                                    tint = SuccessGreen
                                )

                                DeliveryState.CANCELLED, DeliveryState.FAILED -> Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Not delivered",
                                    tint = MaterialTheme.colorScheme.error
                                )

                                else -> Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Other",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Order details: date + order ID
                        Column(
                            modifier = Modifier.width(detailsWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = log.orderDate,
                                color = MaterialTheme.colorScheme.onBackground,
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
                            color = when (log.state) {
                                DeliveryState.DELIVERED -> SuccessGreen
                                DeliveryState.CANCELLED, DeliveryState.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.width(timeWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}