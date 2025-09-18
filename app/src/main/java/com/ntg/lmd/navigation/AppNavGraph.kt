@file:Suppress("DEPRECATION")

package com.ntg.lmd.navigation

import android.content.Intent
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.R
import com.ntg.lmd.navigation.component.drawerHost
import com.ntg.lmd.navigation.component.navigateSingleTop
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
import com.ntg.lmd.notification.ui.viewmodel.NotificationsViewModel
import com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.getViewModel
import com.ntg.lmd.authentication.ui.screens.login.loginScreen as LoginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen as RegisterScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen as SplashScreen

@Composable
fun appNavGraph(
    rootNavController: NavHostController,
    deeplinkVM: DeepLinkViewModel,
    notificationsVM: NotificationsViewModel,
) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            handleSplashNavigation(rootNavController, deeplinkVM, notificationsVM)
        }

        composable(Screen.Login.route) { LoginScreen(navController = rootNavController) }
        composable(Screen.Register.route) { RegisterScreen(navController = rootNavController) }

        composable(
            route = Screen.Drawer.route + "?openNotifications={openNotifications}",
            arguments =
                listOf(
                    navArgument("openNotifications") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            deepLinks = listOf(navDeepLink { uriPattern = "myapp://notifications" }),
        ) { backStackEntry ->
            handleDrawerHost(backStackEntry, rootNavController)
        }
    }
}

@Composable
private fun handleSplashNavigation(
    navController: NavHostController,
    deeplinkVM: DeepLinkViewModel,
    notificationsVM: NotificationsViewModel,
) {
    SplashScreen(
        navController = navController,
        notificationsVM = notificationsVM,
        onDecide = { isLoggedIn ->
            val wantsNotifications = deeplinkVM.consumeOpenNotifications()
            navController.navigate(
                when {
                    !isLoggedIn -> Screen.Login.route
                    wantsNotifications -> "${Screen.Drawer.route}?openNotifications=true"
                    else -> Screen.Drawer.route
                },
            ) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        },
    )
}

@Composable
private fun handleDrawerHost(
    backStackEntry: NavBackStackEntry,
    rootNavController: NavHostController,
) {
    val argOpen = backStackEntry.arguments?.getBoolean("openNotifications") ?: false

    val deepLinkIntent: Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backStackEntry.arguments?.getParcelable("android-support-nav:controller:deepLinkIntent", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            backStackEntry.arguments?.getParcelable("android-support-nav:controller:deepLinkIntent")
        }

    val uri = deepLinkIntent?.data
    val deepOpen = (uri?.scheme == "myapp" && uri.host == "notifications")

    val settingsVm: SettingsViewModel = getViewModel()

    val logoutState by settingsVm.logoutState.collectAsState()

    LaunchedEffect(logoutState) {
        when (logoutState) {
            is com.ntg.lmd.settings.data.LogoutUiState.Success -> {
                rootNavController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Drawer.route) { inclusive = true }
                    launchSingleTop = true
                }
                settingsVm.resetLogoutState()
            }
            is com.ntg.lmd.settings.data.LogoutUiState.Error -> {
                settingsVm.resetLogoutState()
            }
            else -> Unit
        }
    }

    drawerHost(
        onLogout = { settingsVm.logout() },
        openNotifications = argOpen || deepOpen,
    )
}

@Composable
fun buildRouteUiSpec(
    currentRoute: String,
    nav: NavHostController,
    openOrdersHistoryMenu: (() -> Unit)?,
): RouteUiSpec =
    when (currentRoute) {
        Screen.GeneralPool.route -> routeSpecGeneralPool(nav)
        Screen.MyPool.route -> routeSpecMyPool(nav)
        Screen.MyOrders.route -> routeSpecTitleOnly(R.string.menu_my_orders, true, R.string.search_order_number)
        Screen.DeliveriesLog.route ->
            routeSpecTitleOnly(R.string.menu_deliveries_log, true, R.string.search_order_number)
        Screen.OrdersHistory.route ->
            RouteUiSpec(
                title = stringResource(R.string.menu_order_history),
                showSearchIcon = false,
                actionIcon = Icons.Filled.MoreVert,
                onActionIconClick = { openOrdersHistoryMenu?.invoke() },
            )
        Screen.Notifications.route -> routeSpecTitleOnly(R.string.menu_notifications, false)
        Screen.Settings.route -> routeSpecTitleOnly(R.string.menu_settings, false)
        Screen.Chat.route -> routeSpecTitleOnly(R.string.menu_chat, false)
        else -> routeSpecTitleOnly(R.string.app_name, false)
    }

@Composable
private fun routeSpecGeneralPool(nav: NavHostController) =
    RouteUiSpec(
        title = stringResource(R.string.menu_general_pool),
        showSearchIcon = true,
        searchPlaceholder = stringResource(R.string.search_order_number_customer_name),
        actionButtonLabel = stringResource(R.string.my_pool),
        onActionButtonClick = { nav.navigateSingleTop(Screen.MyPool.route) },
    )

@Composable
private fun routeSpecMyPool(nav: NavHostController) =
    RouteUiSpec(
        title = stringResource(R.string.my_pool),
        showSearchIcon = false,
        actionButtonLabel = stringResource(R.string.menu_general_pool),
        onActionButtonClick = { nav.navigateSingleTop(Screen.GeneralPool.route) },
    )

@Composable
private fun routeSpecTitleOnly(
    titleRes: Int,
    showSearchIcon: Boolean,
    placeholderRes: Int? = null,
) = RouteUiSpec(
    title = stringResource(titleRes),
    showSearchIcon = showSearchIcon,
    searchPlaceholder = placeholderRes?.let { stringResource(it) },
)
