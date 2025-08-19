package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.navigation.Screen

@Composable
fun generalPoolScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "General Pool Screen")

            Button(
                onClick = {
                    navController.navigate(Screen.DeliveriesLog.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("Deliveries Log") }

            Button(
                onClick = {
                    navController.navigate(Screen.MyOrders.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("My Orders") }

            Button(
                onClick = {
                    navController.navigate(Screen.MyPool.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("My Pool") }

            Button(
                onClick = {
                    navController.navigate(Screen.OrdersHistory.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("Orders History") }

            Button(
                onClick = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("Settings") }

            Button(
                onClick = {
                    navController.navigate(Screen.Notifications.route) {
                        popUpTo(Screen.GeneralPool.route) { inclusive = true }
                    }
                },
            ) { Text("Notifications") }
        }
    }
}
