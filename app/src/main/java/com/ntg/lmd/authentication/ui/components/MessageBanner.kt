package com.ntg.lmd.authentication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun messageBanner(
    messageRes: Int?,
    messageText: String?,
) {
    val textToShow = messageText ?: messageRes?.let { stringResource(it) }
    AnimatedVisibility(
        visible = !textToShow.isNullOrEmpty(),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        textToShow?.let { txt ->
            AssistChip(onClick = {}, label = { Text(txt) })
        }
    }
}
