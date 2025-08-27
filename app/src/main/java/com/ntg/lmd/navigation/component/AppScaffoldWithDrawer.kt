package com.ntg.lmd.navigation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ntg.lmd.MyApp
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.AppNavConfig
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.navigation.TopBarConfigWithTitle
import com.ntg.lmd.ui.theme.CupertinoCellBackground
import com.ntg.lmd.ui.theme.CupertinoLabelPrimary
import com.ntg.lmd.ui.theme.CupertinoLabelSecondary
import com.ntg.lmd.ui.theme.CupertinoSeparator
import com.ntg.lmd.ui.theme.CupertinoSystemBackground
import kotlinx.coroutines.launch

// ---------- Constants ----------
const val ENABLED_ICON = 1f
const val DISABLED_ICON = 0.38f
private const val FIRST_GROUP_SIZE = 3
private const val FADE_ANIMATION_DURATION_MS = 280

// ---------- Navigation Helper ----------
// Extension function on NavHostController to navigate without creating multiple copies
fun NavHostController.navigateSingleTop(route: String) {
    this.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(this@navigateSingleTop.graph.startDestinationId) { saveState = true }
    }
}

/**
 * Bundle app-bar/search params to avoid LongParameterList.
 */
data class AppBarConfig(
    val title: String,
    val showSearch: Boolean = false,
    val searchValue: String = "",
    val onSearchChange: (String) -> Unit = {},
)
@Suppress("UnusedParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appScaffoldWithDrawer(
    navConfig: AppNavConfig, // holds navController + currentRoute
    topBar: TopBarConfigWithTitle, // UI config for top bar
    appBar: AppBarConfig, // simple title/search config
    onLogout: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navController = navConfig.navController
    val currentRoute = navConfig.currentRoute
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val app = LocalContext.current.applicationContext as MyApp
    val online by app.networkMonitor.isOnline.collectAsState()
    LaunchedEffect(online) {
    }
    // Modal drawer wraps the entire screen with a navigation drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        // Disable gestures on GeneralPool to avoid conflicts with the map
        gesturesEnabled =
            currentRoute !in
                listOf(
                    Screen.GeneralPool.route,
                    Screen.MyPool.route,
                ),
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = CupertinoSystemBackground) {
                drawerContent(
                    currentRoute = currentRoute,
                    onLogout = onLogout,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route == Screen.Logout.route) {
                            onLogout()
                        } else {
                            navController.navigateSingleTop(route)
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                appTopBar(
                    config = topBar,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                )
            },
        ) { inner -> Box(Modifier.padding(inner)) { content() } }
    }
}

@Composable
private fun drawerContent(
    currentRoute: String?,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // Split drawer items into 2 groups (orders + settings/logout)
    val grouped =
        remember {
            Pair(drawerItems.take(FIRST_GROUP_SIZE), drawerItems.drop(FIRST_GROUP_SIZE))
        }

    drawerHeader(name = "Sherif")

    // Section title ("Orders")
    Text(
        text = stringResource(R.string.drawer_section_orders),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = dimensionResource(R.dimen.drawer_section_title_text_size).value.sp,
        modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.mediumSpace),
                top = dimensionResource(R.dimen.smallSpace),
                bottom = dimensionResource(R.dimen.smallerSpace),
            ),
    )

    // First group of items
    groupCard {
        grouped.first.forEachIndexed { i, item ->
            drawerItemRow(entry = item, selected = currentRoute == item.route) {
                if (item.route == Screen.Logout.route) onLogout() else onNavigate(item.route)
            }
            if (i != grouped.first.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.drawer_divider_inset)),
                    thickness = dimensionResource(R.dimen.hairline),
                    color = CupertinoSeparator,
                )
            }
        }
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))

    // Second group of items (Settings, Logout, etc.)
    groupCard {
        grouped.second.forEachIndexed { i, item ->
            drawerItemRow(entry = item, selected = currentRoute == item.route) {
                if (item.route == Screen.Logout.route) onLogout() else onNavigate(item.route)
            }
            if (i != grouped.second.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.drawer_divider_inset)),
                    thickness = dimensionResource(R.dimen.hairline),
                    color = CupertinoSeparator,
                )
            }
        }
    }
}

// ---------- Top App Bar ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appTopBar(
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
    val fadeSpec = remember { tween<Float>(FADE_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing) }

    LaunchedEffect(search.searching.value) {
        if (search.searching.value) focusRequester.requestFocus()
    }

    TopAppBar(
        colors = colors,
        navigationIcon = {
            if (!search.searching.value) {
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
        },
        title = {
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
        },
        actions = { topBarActions(config = config, searching = search.searching.value) },
    )
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

@Composable
private fun searchTextField(
    search: SearchController,
    placeholder: String,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    TextField(
        value = search.text.value,
        onValueChange = search.onTextChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = onPrimary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                if (search.text.value.isNotEmpty()) {
                    search.onTextChange("")
                } else {
                    search.onToggle(false)
                    focusManager.clearFocus()
                }
            }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clear_or_close),
                    tint = onPrimary,
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    search.onSubmit(search.text.value)
                    focusManager.clearFocus()
                },
            ),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Transparent,
                unfocusedIndicatorColor = Transparent,
                disabledIndicatorColor = Transparent,
                errorIndicatorColor = Transparent,
                focusedContainerColor = Transparent,
                unfocusedContainerColor = Transparent,
                disabledContainerColor = Transparent,
                errorContainerColor = Transparent,
                cursorColor = onPrimary,
                focusedTextColor = onPrimary,
                unfocusedTextColor = onPrimary,
            ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = onPrimary),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
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
                Icon(icon, contentDescription = stringResource(R.string.action), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(Modifier.width(4.dp))
        }
        config.searchActionIcon?.let { icon ->
            IconButton(onClick = { config.onSearchIconClick?.invoke() }) {
                Icon(icon, contentDescription = stringResource(R.string.search_order_number_customer_name))
            }
        }
    }
}

// ---------- Drawer Header ----------
@Composable
fun drawerHeader(name: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .height(dimensionResource(R.dimen.drawer_header_height))
                .padding(
                    horizontal = dimensionResource(R.dimen.mediumSpace),
                    vertical = dimensionResource(R.dimen.mediumSpace),
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_user_placeholder),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.drawer_avatar_size)).clip(CircleShape),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(R.dimen.drawer_header_text_size).value.sp,
            )
        }
    }
}

// ---------- Group Card Wrapper ----------
@Composable
private fun groupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = dimensionResource(R.dimen.smallSpace))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.card_radius)))
                .background(CupertinoCellBackground)
                .padding(vertical = dimensionResource(R.dimen.smallSpace)),
        content = content,
    )
}

// ---------- Drawer Item Row ----------
@Composable
fun drawerItemRow(
    entry: DrawerItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val textColor = if (entry.enabled) CupertinoLabelPrimary else CupertinoLabelSecondary
    val iconAlpha = if (entry.enabled) ENABLED_ICON else DISABLED_ICON
    val label = stringResource(entry.labelRes)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = entry.enabled, onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.mediumSpace),
                    vertical = dimensionResource(R.dimen.smallSpace),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = textColor,
            modifier =
                Modifier
                    .size(dimensionResource(R.dimen.drawer_icon_size))
                    .graphicsLayer(alpha = iconAlpha),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))

        Text(
            text = label,
            color = textColor,
            fontSize = dimensionResource(R.dimen.drawer_item_text_size).value.sp,
            modifier = Modifier.weight(1f),
        )

        entry.badgeCount?.let {
            Text(
                text = it.toString(),
                color = CupertinoLabelSecondary,
                fontSize = dimensionResource(R.dimen.drawer_badge_text_size).value.sp,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        }

        if (entry.enabled && entry.route != Screen.Logout.route) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = CupertinoLabelSecondary,
            )
        }
    }

    if (selected) {
        HorizontalDivider(
            thickness = dimensionResource(R.dimen.hairline),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        )
    }
}
