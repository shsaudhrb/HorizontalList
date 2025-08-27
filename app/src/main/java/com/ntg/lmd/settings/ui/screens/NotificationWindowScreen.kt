package com.ntg.lmd.settings.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun notificationWindowScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    onApplyWindowDays: (Int) -> Unit = {},
) {
    val ui by vm.ui.collectAsState()
    var temp by remember { mutableStateOf(ui.window) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(dimensionResource(R.dimen.mediumSpace)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace)),
        ) {
            radioLine(stringResource(R.string.notif_every_15_days), temp == NotificationWindow.D15) {
                temp = NotificationWindow.D15
            }
            radioLine(stringResource(R.string.notif_every_30_days), temp == NotificationWindow.D30) {
                temp = NotificationWindow.D30
            }
            radioLine(stringResource(R.string.notif_every_90_days), temp == NotificationWindow.D90) {
                temp = NotificationWindow.D90
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallSpace)),
            ) {
                gradientPrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.apply),
                    loading = false,
                    onClick = {
                        vm.setNotificationWindow(temp)
                        onApplyWindowDays(temp.days)
                        onBack()
                    },
                )
            }
        }
    }
}
