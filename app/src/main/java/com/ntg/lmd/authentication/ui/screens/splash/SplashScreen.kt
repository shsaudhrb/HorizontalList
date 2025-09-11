package com.ntg.lmd.authentication.ui.screens.splash

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.authentication.ui.components.NotificationPermissionState
import com.ntg.lmd.authentication.ui.components.notificationPermissionHandler
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.notification.ui.viewmodel.NotificationsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun splashScreen(
    navController: NavController,
    onDecide: (Boolean) -> Unit,
    notificationsVM: NotificationsViewModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // first time
    notificationPermissionHandler { state ->
        notificationsVM.updatePermissionState(state)
    }

    // listen
    observeNotificationPermission(
        notificationsVM = notificationsVM,
        snackbarHostState = snackbarHostState,
        scope = scope,
        context = context,
        onDecide = onDecide,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Splash Screen")
                splashButtons(navController)
            }
        }
    }
}

@Composable
private fun observeNotificationPermission(
    notificationsVM: NotificationsViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onDecide: (Boolean) -> Unit,
) {
    LaunchedEffect(Unit) {
        notificationsVM.permissionState.collect { state ->
            when (state) {
                NotificationPermissionState.GRANTED -> onDecide(false)

                NotificationPermissionState.DENIED -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "We need notification permission to send you updates",
                            actionLabel = "Continue",
                        )
                    }
                }

                NotificationPermissionState.DENIED_PERMANENTLY -> {
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                "Notifications are disabled permanently. Please enable them from Settings.",
                                actionLabel = "Open Settings",
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                )
                            context.startActivity(intent)
                        }
                    }
                }

                NotificationPermissionState.SYSTEM_DENIED -> onDecide(false)
            }
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
