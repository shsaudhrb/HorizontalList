package com.ntg.lmd.navigation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ntg.lmd.MyApp
import com.ntg.lmd.R
import com.ntg.lmd.navigation.AppNavConfig
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.navigation.TopBarConfigWithTitle
import com.ntg.lmd.ui.theme.CupertinoSeparator
import com.ntg.lmd.ui.theme.CupertinoSystemBackground
import kotlinx.coroutines.launch

const val ENABLED_ICON = 1f
const val DISABLED_ICON = 0.38f
private const val FIRST_GROUP_SIZE = 3

fun NavHostController.navigateSingleTop(route: String) {
    this.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(this@navigateSingleTop.graph.startDestinationId) { saveState = true }
    }
}

data class DrawerScaffoldConfig(
    val navConfig: AppNavConfig,
    val topBar: TopBarConfigWithTitle,
    val onLogout: () -> Unit,
    val userName: String?,
    val showChrome: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appScaffoldWithDrawer(
    config: DrawerScaffoldConfig,
    content: @Composable () -> Unit,
) {
    val navController = config.navConfig.navController
    val currentRoute = config.navConfig.currentRoute
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val app = LocalContext.current.applicationContext as MyApp
    val online by app.networkMonitor.isOnline.collectAsState()
    LaunchedEffect(online) { }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute?.startsWith(Screen.GeneralPool.route) != true,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = CupertinoSystemBackground) {
                drawerContent(
                    currentRoute = currentRoute,
                    displayName = config.userName ?: "—",
                    onLogout = config.onLogout,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route == Screen.Logout.route) config.onLogout() else navController.navigateSingleTop(route)
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (config.showChrome) {
                    appTopBar(
                        config = config.topBar,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                }
            },
        ) { inner -> Box(Modifier.padding(inner)) { content() } }
    }
}

@Composable
private fun drawerContent(
    currentRoute: String?,
    displayName: String?,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val grouped = remember { Pair(drawerItems.take(FIRST_GROUP_SIZE), drawerItems.drop(FIRST_GROUP_SIZE)) }

    drawerHeader(name = displayName ?: "—")

    drawerSection(
        titleRes = R.string.drawer_section_orders,
        items = grouped.first,
        currentRoute = currentRoute,
        onLogout = onLogout,
        onNavigate = onNavigate,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.smallSpace)))

    drawerSection(
        titleRes = null,
        items = grouped.second,
        currentRoute = currentRoute,
        onLogout = onLogout,
        onNavigate = onNavigate,
    )
}

@Composable
private fun drawerSection(
    titleRes: Int?,
    items: List<DrawerItem>,
    currentRoute: String?,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    if (titleRes != null) {
        Text(
            text = stringResource(titleRes),
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
    }

    groupCard {
        items.forEachIndexed { i, item ->
            drawerItemRow(entry = item, selected = currentRoute == item.route) {
                if (item.route == Screen.Logout.route) onLogout() else onNavigate(item.route)
            }
            if (i != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.drawer_divider_inset)),
                    thickness = dimensionResource(R.dimen.hairline),
                    color = CupertinoSeparator,
                )
            }
        }
    }
}
