package com.ntg.lmd.authentication.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.navigation.Screen

@Composable
fun splashScreen(
    navController: NavController,
    onDecide: (Boolean) -> Unit,
) {
    LaunchedEffect(Unit) {
        val isLoggedIn = false
        onDecide(isLoggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Splash Screen")
            splashButtons(navController)
        }
    }
}

@Composable
private fun splashButtons(navController: NavController) {
    Button(
        onClick = {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        },
    ) { Text("Go to Login") }

    Button(
        onClick = {
            navController.navigate(Screen.Register.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        },
    ) { Text("Go to Register") }
}
