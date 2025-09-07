package com.ntg.lmd.authentication.ui.screens.splash

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.notification.data.dataSource.remote.FcmNotificationHelper

@Composable
fun splashScreen(
    navController: NavController,
    onDecide: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    // System permission launcher
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { _ ->
            // Regardless of granted or denied → continue
            onDecide(false)
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ → request notification permission directly
            if (!FcmNotificationHelper.canPostNotifications(context)) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onDecide(false)
            }
        } else {
            // Old Android versions → no permission needed
            onDecide(false)
        }
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
