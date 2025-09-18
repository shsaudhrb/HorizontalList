package com.ntg.lmd.authentication.ui.screens.login

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.components.appLogo
import com.ntg.lmd.authentication.ui.components.authCard
import com.ntg.lmd.authentication.ui.components.forgotPasswordLink
import com.ntg.lmd.authentication.ui.components.gradientPrimaryButton
import com.ntg.lmd.authentication.ui.components.messageBanner
import com.ntg.lmd.authentication.ui.components.passwordField
import com.ntg.lmd.authentication.ui.components.usernameField
import com.ntg.lmd.authentication.ui.model.CardUi
import com.ntg.lmd.authentication.ui.model.InputProps
import com.ntg.lmd.authentication.ui.model.LoginUiState
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.navigation.Screen
import org.koin.androidx.compose.koinViewModel

private const val CARD_SCALE_FOCUSED = 1.02f
private const val CARD_SCALE_DEFAULT = 1f
private const val CARD_ELEVATION_FOCUSED = 10f
private const val CARD_ELEVATION_DEFAULT = 2f

@Composable
fun loginScreen(navController: NavController) {
    val viewModel: LoginViewModel = koinViewModel()
    val ui = collectLoginUi(viewModel)
    val focusManager = LocalFocusManager.current
    val (cardUi, onUsernameFocus, onPasswordFocus) = rememberCardAndFocusHandlers(viewModel)

    BoxWithConstraints {
        val isTablet = maxWidth > 600.dp

        provideLoginFocusLocals(
            onUsernameFocus = onUsernameFocus,
            onPasswordFocus = onPasswordFocus,
        ) {
            if (isTablet) {
                tabletLoginLayout(navController, ui, viewModel, focusManager, cardUi)
            } else {
                phoneLoginLayout(navController, ui, viewModel, focusManager, cardUi)
            }
        }
    }
}

@Composable
private fun collectLoginUi(viewModel: LoginViewModel): LoginUiState {
    val ui by viewModel.uiState.collectAsState()
    return ui
}

@Composable
fun rememberCardAndFocusHandlers(viewModel: LoginViewModel): Triple<CardUi, (Boolean) -> Unit, (Boolean) -> Unit> {
    var anyFieldFocused by remember { mutableStateOf(false) }

    val transition = updateTransition(targetState = anyFieldFocused, label = "focusTransition")
    val scale by transition.animateFloat(label = "cardScale") {
        if (it) CARD_SCALE_FOCUSED else CARD_SCALE_DEFAULT
    }
    val elevation by transition.animateFloat(label = "cardElevation") {
        if (it) CARD_ELEVATION_FOCUSED else CARD_ELEVATION_DEFAULT
    }
    val cardUi = remember(scale, elevation) { CardUi(scale = scale, elevation = elevation) }

    val onUsernameFocus: (Boolean) -> Unit = {
        anyFieldFocused = it
        viewModel.updateUsernameFocus(it)
    }
    val onPasswordFocus: (Boolean) -> Unit = {
        anyFieldFocused = it
        viewModel.updatePasswordFocus(it)
    }
    return Triple(cardUi, onUsernameFocus, onPasswordFocus)
}

@Composable
private fun provideLoginFocusLocals(
    onUsernameFocus: (Boolean) -> Unit,
    onPasswordFocus: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalOnFocusForUsername provides onUsernameFocus,
        LocalOnFocusForPassword provides onPasswordFocus,
    ) { content() }
}

@Composable
fun loginScaffold(
    card: CardUi,
    messageRes: Int?,
    messageText: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensionResource(R.dimen.extralargeSpace)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            appLogo()
            messageBanner(messageRes = messageRes, messageText = messageText)
            authCard(
                cardScale = card.scale,
                cardElevation = card.elevation,
                content = content,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))
        }
    }
}

// Card Fields & Button
@Composable
fun authFields(
    navController: NavController,
    ui: LoginUiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    credentialsInputs(ui, onUsername, onPassword)
    forgotPasswordLink(enabled = !ui.isLoading)
    Spacer(Modifier.height(dimensionResource(R.dimen.largeSpace)))
    loginButton(loading = ui.isLoading, onSubmit = onSubmit)
    loginSuccessHandler(loginSuccess = ui.loginSuccess, navController = navController)
}

@Composable
private fun credentialsInputs(
    ui: LoginUiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
) {
    usernameField(
        props =
            InputProps(
                value = ui.username,
                onValueChange = onUsername,
                label = R.string.username,
                placeholder = R.string.agentId,
                errorResId = ui.usernameError?.resId,
                showError = ui.showUsernameError,
                onFocusChange = LocalOnFocusForUsername.current,
            ),
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))
    passwordField(
        props =
            InputProps(
                value = ui.password,
                onValueChange = onPassword,
                label = R.string.password,
                placeholder = R.string.password_placeholder,
                errorResId = ui.passwordError?.resId,
                showError = ui.showPasswordError,
                onFocusChange = LocalOnFocusForPassword.current,
            ),
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
}

@Composable
private fun loginButton(
    loading: Boolean,
    onSubmit: () -> Unit,
) {
    gradientPrimaryButton(
        text = stringResource(R.string.login),
        loading = loading,
        onClick = onSubmit,
    )
}

@Composable
private fun loginSuccessHandler(
    loginSuccess: Boolean,
    navController: NavController,
) {
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            navController.navigate(Screen.Drawer.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

private val LocalOnFocusForUsername = compositionLocalOf<(Boolean) -> Unit> { {} }
private val LocalOnFocusForPassword = compositionLocalOf<(Boolean) -> Unit> { {} }
