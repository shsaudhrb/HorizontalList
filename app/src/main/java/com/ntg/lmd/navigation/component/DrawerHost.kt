package com.ntg.lmd.navigation.component

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntg.lmd.MyApp
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModelFactory
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.AppNavConfig
import com.ntg.lmd.navigation.RouteUiSpec
import com.ntg.lmd.navigation.Screen
import com.ntg.lmd.navigation.TopBarConfigWithTitle
import com.ntg.lmd.navigation.buildRouteUiSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun drawerHost(
    onLogout: () -> Unit,
    openNotifications: Boolean = false,
) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route
    val startDest = if (openNotifications) Screen.Notifications.route else Screen.GeneralPool.route

    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyApp

    val loginVm =
        viewModel<LoginViewModel>(
            factory = LoginViewModelFactory(ctx.applicationContext as Application),
        )
    val loginUi by loginVm.uiState.collectAsState()
    val effectiveUserName = loginUi.displayName ?: app.authRepo.lastLoginName

    var openOrdersHistoryMenu by remember { mutableStateOf<(() -> Unit)?>(null) }

    val spec = buildRouteUiSpec(currentRoute, drawerNavController, openOrdersHistoryMenu)
    val search = rememberSearchController(drawerNavController)
    val topBar = buildTopBar(spec, search)
    val inSettingsSub by rememberSettingsSub(backStack).collectAsState()
    val showChrome = !currentRoute.startsWith(Screen.Settings.route) || !inSettingsSub

    appScaffoldWithDrawer(
        config =
            DrawerScaffoldConfig(
                navConfig = AppNavConfig(drawerNavController, currentRoute),
                topBar = topBar,
                onLogout = onLogout,
                userName = effectiveUserName,
                showChrome = showChrome,
            ),
    ) {
        drawerNavGraph(
            navController = drawerNavController,
            startDestination = startDest,
            registerOpenMenu = { openOrdersHistoryMenu = it },
            onOpenOrderDetails = { id -> drawerNavController.navigate("order/$id") },
        )
    }
}

@Composable
private fun rememberSettingsSub(backStack: NavBackStackEntry?): StateFlow<Boolean> =
    remember(backStack) {
        backStack?.savedStateHandle?.getStateFlow("settings_in_sub", false)
            ?: MutableStateFlow(false)
    }

@Composable
private fun rememberSearchController(navController: NavHostController): SearchController {
    val searching = remember { mutableStateOf(false) }
    val text = remember { mutableStateOf("") }

    return remember(searching, text) {
        SearchController(
            searching = searching,
            text = text,
            onSubmit = { q ->
                navController.currentBackStackEntry?.savedStateHandle?.set("search_submit", q)
            },
            onToggle = { enabled ->
                searching.value = enabled
                navController.currentBackStackEntry?.savedStateHandle?.set("searching", enabled)
            },
            onTextChange = { t ->
                text.value = t
                navController.currentBackStackEntry?.savedStateHandle?.set("search_text", t)
            },
        )
    }
}

@Composable
private fun buildTopBar(
    spec: RouteUiSpec,
    search: SearchController,
): TopBarConfigWithTitle =
    TopBarConfigWithTitle(
        title = spec.title,
        search = search,
        showSearchIcon = spec.showSearchIcon,
        actionButtonLabel = spec.actionButtonLabel,
        onActionButtonClick = spec.onActionButtonClick,
        actionIcon = spec.actionIcon,
        onActionIconClick = spec.onActionIconClick,
        searchPlaceholder = spec.searchPlaceholder,
        searchActionIcon = if (spec.showSearchIcon) Icons.Filled.Search else null,
        onSearchIconClick = { search.onToggle(true) },
    )
