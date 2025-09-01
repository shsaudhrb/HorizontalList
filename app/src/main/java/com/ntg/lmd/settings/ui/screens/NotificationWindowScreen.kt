package com.ntg.lmd.settings.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.components.gradientPrimaryButton
import com.ntg.lmd.settings.ui.viewmodel.NotificationWindow
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel

@Composable
fun notificationWindowScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onApplyWindowDays: (Int) -> Unit = {},
) {
    val ui by vm.ui.collectAsState()
    var temp by remember { mutableStateOf(ui.window) }

    Scaffold(
        topBar = { NotificationTopBar(onBack) },
    ) { padding ->
        NotificationOptions(
            modifier = Modifier
                .padding(padding)
                .padding(dimensionResource(R.dimen.mediumSpace)),
            selected = temp,
            onSelect = { temp = it },
            onApply = {
                vm.setNotificationWindow(temp)
                onApplyWindowDays(temp.days)
                onBack()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.notification_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun NotificationOptions(
    modifier: Modifier = Modifier,
    selected: NotificationWindow,
    onSelect: (NotificationWindow) -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace)),
    ) {
        radioLine(
            label = stringResource(R.string.notif_every_15_days),
            selected = selected == NotificationWindow.D15,
            onPick = { onSelect(NotificationWindow.D15) },
        )
        radioLine(
            label = stringResource(R.string.notif_every_30_days),
            selected = selected == NotificationWindow.D30,
            onPick = { onSelect(NotificationWindow.D30) },
        )
        radioLine(
            label = stringResource(R.string.notif_every_90_days),
            selected = selected == NotificationWindow.D90,
            onPick = { onSelect(NotificationWindow.D90) },
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallSpace)),
        ) {
            gradientPrimaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.apply),
                loading = false,
                onClick = onApply,
            )
        }
    }
}
