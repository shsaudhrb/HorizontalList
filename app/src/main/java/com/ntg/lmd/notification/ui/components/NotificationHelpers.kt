package com.ntg.lmd.notification.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import kotlinx.coroutines.delay

@Composable
internal fun errorSection(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
internal fun emptySection() {
    val iconSize = dimensionResource(R.dimen.appLogoSize)
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
        Text(
            stringResource(R.string.empty_notifications),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@SuppressLint("AutoboxingStateCreation")
@Composable
internal fun rememberNowMillis(tickMillis: Long): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickMillis) {
        while (true) {
            delay(tickMillis)
            now = System.currentTimeMillis()
        }
    }
    return now
}
