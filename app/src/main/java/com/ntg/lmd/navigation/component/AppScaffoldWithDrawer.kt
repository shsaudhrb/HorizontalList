package com.ntg.lmd.navigation.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ntg.lmd.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appScaffoldWithDrawer(
    config: AppScaffoldConfig,
    actions: AppScaffoldActions,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            drawerContent(
                userName = config.userName.ifBlank { stringResource(R.string.drawer_user_placeholder) },
                currentRoute = config.currentRoute,
                onEntryClick = { route ->
                    scope.launch { drawerState.close() }
                    if (route == Screen.Logout.route) actions.onLogout() else actions.onNavigate(route)
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                appTopBar(
                    title = config.title,
                    showOrdersMenu = config.showOrdersMenu && actions.onOrdersMenuClick != null,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOrdersMenuClick = actions.onOrdersMenuClick,
                )
            },
        ) { inner ->
            Box(Modifier.padding(inner)) { content() }
        }
    }
}

@Composable
private fun drawerContent(
    userName: String,
    currentRoute: String,
    onEntryClick: (String) -> Unit,
) {
    val grouped =
        remember {
            Pair(
                drawerItems.take(FIRST_GROUP_SIZE),
                drawerItems.drop(FIRST_GROUP_SIZE),
            )
        }

    ModalDrawerSheet(drawerContainerColor = CupertinoSystemBackground) {
        drawerHeader(name = userName)

        // Section label
        drawerSectionTitle(text = stringResource(R.string.drawer_section_orders))

        drawerGroup(items = grouped.first, currentRoute = currentRoute, onEntryClick = onEntryClick)

        Spacer(Modifier.height(dimensionResource(R.dimen.space_small)))
                Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))

        drawerGroup(items = grouped.second, currentRoute = currentRoute, onEntryClick = onEntryClick)
    }
}

@Composable
private fun drawerGroup(
    items: List<DrawerItem>,
    currentRoute: String,
    onEntryClick: (String) -> Unit,
) {
    groupCard {
        items.forEachIndexed { index, item ->
            drawerItemRow(
                entry = item,
                selected = currentRoute == item.route,
                onClick = { onEntryClick(item.route) },
            )
            if (index != items.lastIndex) insetDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun appTopBar(
    title: String,
    showOrdersMenu: Boolean,
    onOpenDrawer: () -> Unit,
    onOrdersMenuClick: (() -> Unit)?,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_menu))
            }
        },
        actions = {
            if (showOrdersMenu && onOrdersMenuClick != null) {
                IconButton(onClick = onOrdersMenuClick) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.topbar_more))
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
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
    Divider(
        color = CupertinoSeparator,
        thickness = dimensionResource(R.dimen.hairline),
        modifier = Modifier.padding(start = dimensionResource(R.dimen.drawer_divider_inset)),
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
