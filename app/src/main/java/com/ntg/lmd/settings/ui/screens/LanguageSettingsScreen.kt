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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.components.gradientPrimaryButton
import com.ntg.lmd.settings.ui.viewmodel.AppLanguage
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel
import com.ntg.lmd.utils.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun languageSettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    var selected by remember { mutableStateOf(ui.language) }

    Scaffold(topBar = { languageTopBar(onBack) }) { padding ->
        languageBody(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(dimensionResource(R.dimen.mediumSpace)),
            selected = selected,
            onSelect = { selected = it },
            onApply = {
                vm.setLanguage(it)
                LocaleHelper.applyLanguage(
                    ctx,
                    if (it == AppLanguage.AR) "ar" else "en",
                    recreateActivity = true,
                )
                onBack()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun languageTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_language_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
private fun languageBody(
    modifier: Modifier,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onApply: (AppLanguage) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallerSpace)),
    ) {
        radioLine(
            label = stringResource(R.string.app_language_english),
            selected = selected == AppLanguage.EN,
            onPick = { onSelect(AppLanguage.EN) },
        )
        radioLine(
            label = stringResource(R.string.app_language_arabic),
            selected = selected == AppLanguage.AR,
            onPick = { onSelect(AppLanguage.AR) },
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
                onClick = { onApply(selected) },
            )
        }
    }
}
