package com.ntg.lmd.authentication.ui.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusManager
import androidx.navigation.NavController
import com.ntg.lmd.authentication.ui.model.CardUi
import com.ntg.lmd.authentication.ui.model.LoginUiState
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel

@Composable
fun phoneLoginLayout(
    navController: NavController,
    ui: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: FocusManager,
    cardUi: CardUi,
) {
    loginScaffold(
        card = cardUi,
        messageRes = ui.message,
        messageText = ui.errorMessage,
    ) {
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
}
