package com.ntg.lmd.authentication.ui.screens.login
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.components.appLogo
import com.ntg.lmd.authentication.ui.components.authCard
import com.ntg.lmd.authentication.ui.components.messageBanner
import com.ntg.lmd.authentication.ui.model.CardUi
import com.ntg.lmd.authentication.ui.model.LoginUiState
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel

@Composable
fun tabletLoginLayout(
    navController: NavController,
    ui: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: FocusManager,
    cardUi: CardUi,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensionResource(R.dimen.largestSpace)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.largerSpace)),
        ) {
            tabletLogoColumn()
            tabletLoginFormColumn(navController, ui, viewModel, focusManager, cardUi)
        }
    }
}

@Composable
private fun RowScope.tabletLogoColumn() {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .padding(end = dimensionResource(R.dimen.largeSpace)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        appLogo()
    }
}

@Composable
private fun RowScope.tabletLoginFormColumn(
    navController: NavController,
    ui: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: FocusManager,
    cardUi: CardUi,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        messageBanner(messageRes = ui.message, messageText = ui.errorMessage)
        authCard(cardScale = cardUi.scale, cardElevation = cardUi.elevation) {
            authFields(
                navController = navController,
                ui = ui,
                onUsername = viewModel::updateUsername,
                onPassword = viewModel::updatePassword,
                onSubmit = {
                    focusManager.clearFocus()
                    viewModel.submit()
                },
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.largeSpace)))
    }
}
