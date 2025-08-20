package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun customHeader(
    title: String,
    modifier: Modifier = Modifier,
    showBackIcon: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    showEndIcon: Boolean = true,
    onEndIconClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    onSearchSubmit: (String) -> Unit = {},
    searching: Boolean,
    onSearchingChange: (Boolean) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // focus the text field when search mode is activated
    LaunchedEffect(searching) {
        if (searching) focusRequester.requestFocus()
    }

    // for animation when search bar appear
    val fadeSpec: FiniteAnimationSpec<Float> =
        tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing,
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(backgroundColor)
                .padding(horizontal = 8.dp),
    ) {
        // ---- Normal toolbar (title + icons) ----
        AnimatedVisibility(
            visible = !searching,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
        ) {
            Box(Modifier.fillMaxSize()) {
                // ---- left icon (menu) ----
                if (showBackIcon && onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dehaze,
                            contentDescription = "Menu",
                            tint = contentColor,
                        )
                    }
                }

                // ---- right icon (search) ----
                if (showEndIcon) {
                    IconButton(
                        onClick = {
                            onSearchingChange(true) // open search
                            onEndIconClick?.invoke()
                        },
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = contentColor,
                        )
                    }
                }

                // ---- centered title text ----
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    color = contentColor,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        // ---- Search bar ----
        AnimatedVisibility(
            visible = searching,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // text input for search query
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    // focus automatically
                    singleLine = true,
                    placeholder = {
                        Text("Search…", color = contentColor.copy(alpha = 0.7f))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = contentColor,
                        )
                    },
                    // close button
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (query.isNotEmpty()) {
                                    onQueryChange("") // clear text
                                } else {
                                    onSearchingChange(false) // close search bar
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (query.isNotEmpty()) "Clear" else "Close",
                                tint = contentColor,
                            )
                        }
                    },
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor,
                            cursorColor = contentColor,
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = { onSearchSubmit(query) },
                        ),
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
