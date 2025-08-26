package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.navigation.component.AppHeaderActions
import com.ntg.lmd.navigation.component.appHeader

@Suppress("UnusedParameter")
@Composable
fun orderDetailsScreen(
    orderId: Long,
    navController: NavController,
) {
    Scaffold(
        topBar = {
            appHeader(
                title = "Order Details",
                showBack = true,
                actions =
                    AppHeaderActions(
                        onBackClick = { navController.popBackStack() },
                    ),
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            Text("Order Details", style = MaterialTheme.typography.titleLarge)
        }
    }
}
