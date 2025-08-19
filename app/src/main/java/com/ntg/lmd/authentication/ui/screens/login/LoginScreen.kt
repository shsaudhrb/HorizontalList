package com.ntg.lmd.authentication.ui.screens.login

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
fun loginScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Login Screen")

            Button(
                onClick = {
                    navController.navigate(Screen.EnterEmailResetPassword.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            ) { Text("Reset Password") }

            Button(
                onClick = {
                    navController.navigate(Screen.GeneralPool.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            ) { Text("Main Screens") }
        }
    }
}
