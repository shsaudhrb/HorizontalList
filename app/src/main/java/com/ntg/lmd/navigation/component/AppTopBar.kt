package com.ntg.lmd.navigation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.TopBarConfigWithTitle

private const val FADE_ANIMATION_DURATION_MS = 280

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appTopBar(
    config: TopBarConfigWithTitle,
    onOpenDrawer: () -> Unit,
    colors: TopAppBarColors =
        topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
) {
    val search = config.search
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val fadeSpec =
        remember { tween<Float>(FADE_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing) }

    LaunchedEffect(search.searching.value) {
        if (search.searching.value) focusRequester.requestFocus()
    }

    TopAppBar(
        colors = colors,
        navigationIcon = {
            appTopBarNavigationIcon(
                isSearching = search.searching.value,
                onOpenDrawer = onOpenDrawer,
            )
        },
        title = {
            appTopBarTitleSection(
                config = config,
                search = search,
                fadeSpec = fadeSpec,
                focusRequester = focusRequester,
                focusManager = focusManager,
            )
        },
        actions = { topBarActions(config = config, searching = search.searching.value) },
    )
}

@Composable
private fun appTopBarNavigationIcon(
    isSearching: Boolean,
    onOpenDrawer: () -> Unit,
) {
    if (!isSearching) {
        IconButton(onClick = onOpenDrawer, modifier = Modifier.padding(start = 8.dp)) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
        }
    } else {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = null, // decorative when searching
            tint = MaterialTheme.colorScheme.primary, // keep space, de-emphasize
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun appTopBarTitleSection(
    config: TopBarConfigWithTitle,
    search: SearchController, // adjust type to your actual search config
    fadeSpec: FiniteAnimationSpec<Float>,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
    topBarTitle(
        title = config.title,
        showTitle = !search.searching.value,
        fadeSpec = fadeSpec,
    )
    if (config.showSearchIcon && search.searching.value) {
        searchTextField(
            search = search,
            placeholder =
                config.searchPlaceholder
                    ?: stringResource(R.string.search_order_number_customer_name),
            focusRequester = focusRequester,
            focusManager = focusManager,
        )
    }
}

@Composable
private fun topBarTitle(
    title: String,
    showTitle: Boolean,
    fadeSpec: FiniteAnimationSpec<Float>,
) {
    AnimatedVisibility(visible = showTitle, enter = fadeIn(fadeSpec), exit = fadeOut(fadeSpec)) {
        Text(
            title,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                ),
        )
    }
}

// Right-side action buttons in top app bar
@Composable
private fun topBarActions(
    config: TopBarConfigWithTitle,
    searching: Boolean,
) {
    if (!searching) {
        // Optional text button (MY POOL / GENERAL POOL)
        config.actionButtonLabel?.let { label ->
            TextButton(onClick = { config.onActionButtonClick?.invoke() }) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        // Optional custom icon (e.g. MoreVert)
        config.actionIcon?.let { icon ->
            IconButton(onClick = { config.onActionIconClick?.invoke() }) {
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.action),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        config.searchActionIcon?.let { icon ->
            IconButton(onClick = { config.onSearchIconClick?.invoke() }) {
                Icon(
                    icon,
                    contentDescription = stringResource(R.string.search_order_number_customer_name),
                )
            }
        }
    }
}
