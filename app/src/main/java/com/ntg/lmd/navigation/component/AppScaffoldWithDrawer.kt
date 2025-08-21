package com.ntg.lmd.navigation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.graphicsLayer
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
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.AppNavConfig
import com.ntg.lmd.navigation.AppSearchConfig
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.ui.theme.CupertinoCellBackground
import com.ntg.lmd.ui.theme.CupertinoLabelPrimary
import com.ntg.lmd.ui.theme.CupertinoLabelSecondary
import com.ntg.lmd.ui.theme.CupertinoSeparator
import com.ntg.lmd.ui.theme.CupertinoSystemBackground
import kotlinx.coroutines.launch

const val ENABLED_ICON = 1f
const val DISABLED_ICON = 0.38f
private const val FIRST_GROUP_SIZE = 3
private const val FADE_ANIMATION_DURATION_MS = 280

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appScaffoldWithDrawer(
    navConfig: AppNavConfig,
    title: String,
    onLogout: () -> Unit,
    searchConfig: AppSearchConfig? = null,
    content: @Composable () -> Unit,
) {
    val navController = navConfig.navController
    val currentRoute = navConfig.currentRoute
    val showSearchIcon = searchConfig?.enabled == true

    // Search state
    val searching = searchConfig?.searchingState ?: remember { mutableStateOf(false) }
    val searchText = searchConfig?.searchTextState ?: remember { mutableStateOf("") }
    val submitSearch = searchConfig?.onSearchSubmit ?: {}

    // Drawer + focus/animation
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // search focus + animation
    val focusRequester = remember { FocusRequester() }

    // autofocus when entering search mode
    LaunchedEffect(searching.value) {
        if (searching.value) focusRequester.requestFocus()
    }

    val search =
        remember(searching, searchText) {
            SearchController(
                searching = searching,
                text = searchText,
                onSubmit = {
                    submitSearch(it)
                    navController.currentBackStackEntry?.savedStateHandle?.set("search_submit", it)
                },
                onToggle = { enabled ->
                    searching.value = enabled
                    navController.currentBackStackEntry?.savedStateHandle?.set("searching", enabled)
                },
                onTextChange = {
                    searchText.value = it
                    navController.currentBackStackEntry?.savedStateHandle?.set("search_text", it)
                },
            )
        }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Disable gestures in the General Pool screen to prevent conflicts with the map
        gesturesEnabled = currentRoute?.startsWith(Screen.GeneralPool.route) != true,
        drawerContent = {
            drawerContent(
                currentRoute = currentRoute,
                onLogout = onLogout,
            ) { route ->
                scope.launch { drawerState.close() }
                if (route == Screen.Logout.route) {
                    onLogout()
                } else {
                    navController.navigateSingleTop(
                        route,
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                appTopBar(
                    title = title,
                    showSearchIcon = showSearchIcon,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    search = search,
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
    val grouped =
        remember {
            Pair(drawerItems.take(FIRST_GROUP_SIZE), drawerItems.drop(FIRST_GROUP_SIZE))
        }
    ModalDrawerSheet(drawerContainerColor = CupertinoSystemBackground) {
        drawerHeader(name = "Sherif")
        drawerSectionTitle(stringResource(R.string.drawer_section_orders))

        groupCard {
            grouped.first.forEachIndexed { i, item ->
                drawerItemRow(
                    entry = item,
                    selected = currentRoute == item.route,
                ) { onNavigate(item.route) }
                if (i != grouped.first.lastIndex) insetDivider()
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))

        groupCard {
            grouped.second.forEachIndexed { i, item ->
                drawerItemRow(
                    entry = item,
                    selected = currentRoute == item.route,
                ) {
                    if (item.route == Screen.Logout.route) onLogout() else onNavigate(item.route)
                }
                if (i != grouped.second.lastIndex) insetDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appTopBar(
    title: String,
    showSearchIcon: Boolean,
    onOpenDrawer: () -> Unit,
    search: SearchController,
    colors: TopAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val fadeSpec =
        remember { tween<Float>(FADE_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing) }

    // Auto focus text field when entering search mode
    LaunchedEffect(search.searching.value) {
        if (search.searching.value) focusRequester.requestFocus()
    }

    TopAppBar(
        colors = colors,
        navigationIcon = {
            // Normal mode: show menu button
            if (!search.searching.value) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.padding(start = 8.dp)) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
                }
            } else {
                // Searching mode: show search icon instead
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
        title = {
            // Show title when not searching
            AnimatedVisibility(
                visible = !search.searching.value,
                enter = fadeIn(fadeSpec),
                exit = fadeOut(fadeSpec),
            ) {
                Text(
                    title,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                        ),
                )
            }
            // Show search text field when searching
            AnimatedVisibility(
                visible = showSearchIcon && search.searching.value,
                enter = fadeIn(fadeSpec),
                exit = fadeOut(fadeSpec),
            ) {
                searchTextField(
                    value = search.text.value,
                    onValueChange = search.onTextChange,
                    onSubmit = {
                        search.onSubmit(it)
                        focusManager.clearFocus()
                    },
                    onClose = {
                        if (search.text.value.isNotEmpty()) {
                            search.onTextChange("")
                        } else {
                            search.onToggle(false)
                            focusManager.clearFocus()
                        }
                    },
                    focusRequester = focusRequester,
                )
            }
        },
        actions = {
            // Action search icon (only when not in search mode)
            if (showSearchIcon && !search.searching.value) {
                IconButton(onClick = { search.onToggle(true) }) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                }
            }
        },
    )
}

@Composable
private fun searchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.search),
                color = onPrimary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Clear/Close", tint = onPrimary)
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSubmit(value) },
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
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
    )
}

fun NavHostController.navigateSingleTop(route: String) {
    this.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(this@navigateSingleTop.graph.startDestinationId) { saveState = true }
    }
}

@Composable
fun drawerHeader(name: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
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
                modifier =
                    Modifier
                        .size(dimensionResource(R.dimen.drawer_avatar_size))
                        .clip(CircleShape),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.smallSpace)))
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onError,
                fontWeight = FontWeight.SemiBold,
                fontSize = dimensionResource(R.dimen.drawer_header_text_size).value.sp,
            )
        }
    }
}

@Composable
fun drawerSectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.SemiBold,
        fontSize = dimensionResource(R.dimen.drawer_section_title_text_size).value.sp,
        modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.mediumSpace),
                top = dimensionResource(R.dimen.smallSpace),
                bottom = dimensionResource(R.dimen.smallerSpace),
            ),
    )
}

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

@Composable
private fun insetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = dimensionResource(R.dimen.drawer_divider_inset)),
        thickness = dimensionResource(R.dimen.hairline),
        color = CupertinoSeparator,
    )
}

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
        // Leading icon
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

        // Label
        Text(
            text = label,
            color = textColor,
            fontSize = dimensionResource(R.dimen.drawer_item_text_size).value.sp,
            modifier = Modifier.weight(1f),
        )

        // Optional right count (plain text, not a badge)
        entry.badgeCount?.let {
            Text(
                text = it.toString(),
                color = CupertinoLabelSecondary,
                fontSize = dimensionResource(R.dimen.drawer_badge_text_size).value.sp,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.smallerSpace)))
        }

        // Chevron for navigable rows
        if (entry.enabled && entry.route != Screen.Logout.route) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = CupertinoLabelSecondary,
            )
        }
    }

    if (selected) {
        Divider(
            thickness = dimensionResource(R.dimen.hairline),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        )
    }
}
