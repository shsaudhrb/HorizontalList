package com.ntg.lmd.authentication.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.ntg.lmd.notification.data.dataSource.remote.FcmNotificationHelper

@Composable
fun notificationPermissionHandler(onResult: (NotificationPermissionState) -> Unit) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        onResult(NotificationPermissionState.GRANTED)
        return
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onResult(NotificationPermissionState.GRANTED)
            } else {
                handleDenied(context, onResult)
            }
        }

    LaunchedEffect(Unit) {
        if (!FcmNotificationHelper.canPostNotifications(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(NotificationPermissionState.GRANTED)
        }
    }
}

private fun handleDenied(
    context: Context,
    onResult: (NotificationPermissionState) -> Unit,
) {
    val activity = context.findActivity()
    if (activity != null) {
        val permanentlyDenied =
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        if (permanentlyDenied) {
            onResult(NotificationPermissionState.DENIED_PERMANENTLY)
        } else {
            onResult(NotificationPermissionState.DENIED)
        }
    } else {
        onResult(NotificationPermissionState.SYSTEM_DENIED)
    }
}

enum class NotificationPermissionState {
    GRANTED,
    DENIED,
    DENIED_PERMANENTLY,
    SYSTEM_DENIED,
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
