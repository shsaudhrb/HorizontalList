package com.ntg.lmd.authentication.ui.screens.reset

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
fun enterEmailResetPasswordScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Enter Email For Reset Password")

            Button(
                onClick = {
                    navController.navigate(Screen.VerificationCode.route) {
                        popUpTo(Screen.EnterEmailResetPassword.route) { inclusive = true }
                    }
                },
            ) { Text("Verification Code") }
        }
    }
}
