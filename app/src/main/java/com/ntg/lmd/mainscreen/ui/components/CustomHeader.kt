package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.HeaderUiModel
import com.ntg.lmd.mainscreen.domain.model.SearchController

@Composable
fun customHeader(
    modifier: Modifier = Modifier,
    uiModel: HeaderUiModel,
    search: SearchController,
) {
    // focus the search text field, only when search mode becomes active
    val focusRequester = remember { FocusRequester() }

    // colors for background and text content
    val backgroundColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    // when search mode turns on, request focus so the user can start typing immediately
    LaunchedEffect(search.searching) {
        if (search.searching) focusRequester.requestFocus()
    }

    // for smooth animation when search bar appears
    val fadeSpec: FiniteAnimationSpec<Float> =
        tween(durationMillis = 280, easing = FastOutSlowInEasing)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.header_height))
                .background(backgroundColor)
                .padding(horizontal = 8.dp),
    ) {
        // ---- Normal toolbar (title + icons) ----
        AnimatedVisibility(
            visible = !search.searching,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
        ) {
            Box(Modifier.fillMaxSize()) {
                // ---- start/left icon (menu) ----
                startIcon(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp),
                    uiModel = uiModel,
                    contentColor = contentColor,
                )

                // ---- end/right icon (search) ----
                endIcon(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                    uiModel = uiModel,
                    search = search,
                    contentColor = contentColor,
                )

                // ---- centered title text ----
                Text(
                    text = uiModel.title,
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
            visible = search.searching,
            enter = fadeIn(animationSpec = fadeSpec),
            exit = fadeOut(animationSpec = fadeSpec),
        ) {
            headerSearchBar(
                searchText = search.searchText,
                onSearchTextChange = search.onSearchTextChange,
                onClose = { search.onSearchingChange(false) }, // close search if user taps X with empty text
                onSubmit = search.onSubmit,
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
private fun startIcon(
    modifier: Modifier = Modifier,
    uiModel: HeaderUiModel,
    contentColor: Color,
) {
    if (uiModel.showStartIcon && uiModel.onStartClick != null) {
        IconButton(onClick = uiModel.onStartClick, modifier = modifier) {
            Icon(
                imageVector = Icons.Default.Dehaze,
                contentDescription = "Menu",
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun endIcon(
    modifier: Modifier = Modifier,
    uiModel: HeaderUiModel,
    search: SearchController,
    contentColor: Color,
) {
    if (uiModel.showEndIcon) {
        IconButton(
            onClick = {
                search.onSearchingChange(true)
                uiModel.onEndIconClick?.invoke()
            },
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun headerSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSubmit: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    // color of the text
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.search),
                    color = contentColor.copy(alpha = 0.7f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        // if there's a text, it will clear it
                        if (searchText.isNotEmpty()) {
                            onSearchTextChange("")
                            // if there's no text, it will close the search bar
                        } else {
                            onClose()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (searchText.isNotEmpty()) "Clear" else "Close",
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
                    onSearch = { onSubmit(searchText) },
                ),
        )
        Spacer(Modifier.width(4.dp))
    }
}
