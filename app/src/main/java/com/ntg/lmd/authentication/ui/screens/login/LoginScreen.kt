package com.ntg.lmd.authentication.ui.screens.login

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.components.appLogo
import com.ntg.lmd.authentication.ui.components.gradientPrimaryButton
import com.ntg.lmd.authentication.ui.components.passwordField
import com.ntg.lmd.authentication.ui.components.usernameField
import com.ntg.lmd.authentication.ui.model.CardUi
import com.ntg.lmd.authentication.ui.model.InputProps
import com.ntg.lmd.authentication.ui.model.LoginUiState
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel

private const val CARD_SCALE_FOCUSED = 1.02f
private const val CARD_SCALE_DEFAULT = 1f
private const val CARD_ELEVATION_FOCUSED = 10f
private const val CARD_ELEVATION_DEFAULT = 2f
private const val CARD_ANIMATION_DURATION = 240

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun loginScreen(
    viewModel: LoginViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel(),
) {
    val focus = LocalFocusManager.current
    val ui by viewModel.uiState.collectAsState()
    // Tracks anyFieldFocused locally to drive card animation (scale/elevation).
    var anyFieldFocused by remember { mutableStateOf(false) }

    val cardUi =
        CardUi(
            scale =
                animateFloatAsState(
                    targetValue = if (anyFieldFocused) CARD_SCALE_FOCUSED else CARD_SCALE_DEFAULT,
                    animationSpec = tween(CARD_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                    label = "cardScale",
                ).value,
            elevation =
                animateFloatAsState(
                    targetValue = if (anyFieldFocused) CARD_ELEVATION_FOCUSED else CARD_ELEVATION_DEFAULT,
                    animationSpec = tween(CARD_ANIMATION_DURATION, easing = FastOutSlowInEasing),
                    label = "cardElevation",
                ).value,
        )

    CompositionLocalProvider(
        LocalOnFocusForUsername provides { f ->
            anyFieldFocused = f
            viewModel.updateUsernameFocus(f)
        },
        LocalOnFocusForPassword provides { f ->
            anyFieldFocused = f
            viewModel.updatePasswordFocus(f)
        },
    ) {
        loginScaffold(
            card = cardUi,
            messageRes = ui.message,
        ) {
            authFields(
                ui = ui,
                onUsername = viewModel::updateUsername,
                onPassword = viewModel::updatePassword,
                onSubmit = {
                    focus.clearFocus()
                    viewModel.submit()
                },
            )
        }
    }
}

@Composable
private fun loginScaffold(
    card: CardUi,
    messageRes: Int?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensionResource(R.dimen.extralargeSpace)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            appLogo()
            messageBanner(messageRes)
            authCard(
                cardScale = card.scale,
                cardElevation = card.elevation,
                content = content,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.mediumSpace)))
        }
    }
}

// result message chip
@Composable
private fun messageBanner(messageRes: Int?) {
    AnimatedVisibility(
        visible = messageRes != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        messageRes?.let { id ->
            AssistChip(onClick = {}, label = { Text(stringResource(id)) })
        }
    }
}

// Card with animation
@Composable
private fun authCard(
    cardScale: Float,
    cardElevation: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.cardRoundCorner))
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                }.shadow(cardElevation.dp, shape, clip = false)
                .clip(shape),
        shape = shape,
        border =
            BorderStroke(
                dimensionResource(R.dimen.smallestStrokeWidth),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.padding(dimensionResource(R.dimen.largeSpace))) { content() }
    }
}

// Card Fields & Button
@Composable
private fun authFields(
    ui: LoginUiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = stringResource(R.string.forgotPassword),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(enabled = !ui.isLoading) {},
        )
    }
    Spacer(Modifier.height(dimensionResource(R.dimen.largeSpace)))

    gradientPrimaryButton(
        text = if (ui.isLoading) "Signing in…" else stringResource(R.string.login),
        loading = ui.isLoading,
        onClick = onSubmit,
    )
}

private val LocalOnFocusForUsername = compositionLocalOf<(Boolean) -> Unit> { {} }
private val LocalOnFocusForPassword = compositionLocalOf<(Boolean) -> Unit> { {} }
