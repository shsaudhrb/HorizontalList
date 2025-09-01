package com.ntg.lmd.mainscreen.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun orderDetailsScreen(
    orderId: String?,
    navController: NavController,
) {
    Scaffold { inner ->
        Column(
            modifier =
                Modifier
                    .padding(inner)
                    .padding(16.dp),
        ) {
            Text("Order Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text("Order ID: $orderId", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            // Back to previous screen
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
        }
    }
}
