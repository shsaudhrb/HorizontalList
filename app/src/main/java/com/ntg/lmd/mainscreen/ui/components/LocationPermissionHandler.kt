package com.ntg.lmd.mainscreen.ui.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun locationPermissionHandler(
    onPermissionGranted: (Context) -> Unit,
    onPermissionDenied: (() -> Unit)? = null,
    requestOnLaunch: Boolean = true,
) {
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                onPermissionGranted(context)
            } else {
                onPermissionDenied?.invoke()
            }
        }

    LaunchedEffect(Unit) {
        val granted =
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            onPermissionGranted(context)
        } else if (requestOnLaunch) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
}
