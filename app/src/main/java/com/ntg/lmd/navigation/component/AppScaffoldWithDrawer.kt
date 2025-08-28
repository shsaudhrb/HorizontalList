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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ntg.lmd.MyApp
import com.ntg.lmd.R
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

// ---------- Navigation Helper ----------
// Extension function on NavHostController to navigate without creating multiple copies
fun NavHostController.navigateSingleTop(route: String) {
    this.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(this@navigateSingleTop.graph.startDestinationId) { saveState = true }
    }
}

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
        gesturesEnabled = currentRoute?.startsWith(Screen.GeneralPool.route) != true,
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
                modifier =
                    Modifier
                        .size(dimensionResource(R.dimen.drawer_avatar_size))
                        .clip(CircleShape),
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
