@file:Suppress("DEPRECATION")

package com.ntg.lmd.navigation

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ntg.lmd.MyApp
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModel
import com.ntg.lmd.authentication.ui.viewmodel.login.LoginViewModelFactory
import com.ntg.lmd.mainscreen.domain.model.SearchController
import com.ntg.lmd.navigation.component.AppBarConfig
import com.ntg.lmd.navigation.component.appScaffoldWithDrawer
import com.ntg.lmd.navigation.component.buildRouteUiSpec
import com.ntg.lmd.navigation.component.buildTopBar
import com.ntg.lmd.navigation.component.drawerNavGraph
import com.ntg.lmd.notification.ui.viewmodel.DeepLinkViewModel
import com.ntg.lmd.authentication.ui.screens.login.loginScreen as LoginScreen
import com.ntg.lmd.authentication.ui.screens.register.registerScreen as RegisterScreen
import com.ntg.lmd.authentication.ui.screens.splash.splashScreen as SplashScreen

@Composable
fun appNavGraph(
    rootNavController: NavHostController,
    deeplinkVM: DeepLinkViewModel,
) {
    NavHost(
        navController = rootNavController,
        startDestination = Screen.Splash.route,
    ) {
        // ---------- Splash ----------
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = rootNavController,
                onDecide = { isLoggedIn ->
                    val wantsNotifications = deeplinkVM.consumeOpenNotifications()
                    rootNavController.navigate(
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

        // ---------- Auth ----------
        composable(Screen.Login.route) { LoginScreen(navController = rootNavController) }
        composable(Screen.Register.route) { RegisterScreen(navController = rootNavController) }

        // ---------- Drawer Host (AFTER LOGIN) ----------
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
            val argOpen = backStackEntry.arguments?.getBoolean("openNotifications") ?: false

            val deepLinkIntent: Intent? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    backStackEntry.arguments?.getParcelable(
                        "android-support-nav:controller:deepLinkIntent",
                        Intent::class.java,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    backStackEntry.arguments?.getParcelable("android-support-nav:controller:deepLinkIntent")
                }
            val uri = deepLinkIntent?.data
            val deepOpen = (uri?.scheme == "myapp" && uri.host == "notifications")
            val ctx = LocalContext.current
            val settingsVm: com.ntg.lmd.settings.ui.viewmodel.SettingsViewModel =
                viewModel(factory = com.ntg.lmd.settings.ui.viewmodel.SettingsViewModelFactory(
                    ctx.applicationContext as Application
                ))

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
                        settingsVm.resetLogoutState() // (show snackbar if you have one)
                    }
                    else -> Unit
                }
            }
            drawerHost(
                onLogout = { settingsVm.logout() },
                openNotifications = argOpen || deepOpen,
            )
        }
    }
}

// ======================= Drawer host =======================

@Composable
private fun drawerHost(
    onLogout: () -> Unit,
    openNotifications: Boolean = false,
) {
    val drawerNavController = rememberNavController()
    val backStack by drawerNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.GeneralPool.route
    val startDest = if (openNotifications) Screen.Notifications.route else Screen.GeneralPool.route

    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyApp

    // VM for login state (may be a different instance than the one that set the name)
    val loginVm: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(ctx.applicationContext as Application)
    )
    val loginUi by loginVm.uiState.collectAsState()

    val effectiveUserName = loginUi.displayName ?: app.authRepo.lastLoginName

    // Shared hook for Orders History overflow menu
    var openOrdersHistoryMenu by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Route-specific UI
    val spec = buildRouteUiSpec(currentRoute, drawerNavController, openOrdersHistoryMenu)

    // Shared search controller
    val search = rememberSearchController(drawerNavController)

    // Top bar config
    val topBar = buildTopBar(spec, search)

    // Scaffold + inner nav
    appScaffoldWithDrawer(
        navConfig = AppNavConfig(
            navController = drawerNavController,
            currentRoute = currentRoute,
        ),
        topBar = topBar,
        appBar = AppBarConfig(title = spec.title),
        onLogout = onLogout,
        userName = effectiveUserName,
        content = {
            drawerNavGraph(
                navController = drawerNavController,
                startDestination = startDest,
                registerOpenMenu = { setter -> openOrdersHistoryMenu = setter },
                externalQuery = search.text.value,
                onOpenOrderDetails = { id -> drawerNavController.navigate("order/$id") },
            )
        }
    )
}

// ======================= Helpers =======================

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
