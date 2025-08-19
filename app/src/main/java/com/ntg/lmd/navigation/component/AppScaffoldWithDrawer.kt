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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.ntg.lmd.ui.theme.CupertinoSystemBackground
import com.ntg.lmd.ui.theme.CupertinoCellBackground
import com.ntg.lmd.ui.theme.CupertinoSeparator
import com.ntg.lmd.ui.theme.CupertinoLabelPrimary
import com.ntg.lmd.ui.theme.CupertinoLabelSecondary
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ntg.lmd.R
import com.ntg.lmd.navigation.Screen
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appScaffoldWithDrawer(
    navController: NavHostController,
    currentRoute: String,
    title: String,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CupertinoSystemBackground
            ) {
                drawerHeader(name = "Sherif")

                // Section label
                drawerSectionTitle("ORDERS")

                groupCard {
                    val firstGroup = drawerItems.take(3)
                    firstGroup.forEachIndexed { index, item ->
                        drawerItemRow(
                            entry = item,
                            selected = currentRoute == item.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.route == Screen.Logout.route) onLogout()
                                else navController.navigateSingleTop(item.route)
                            }
                        )
                        if (index != firstGroup.lastIndex) insetDivider()
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Group 2 (rest)
                groupCard {
                    val rest = drawerItems.drop(3)
                    rest.forEachIndexed { index, item ->
                        drawerItemRow(
                            entry = item,
                            selected = currentRoute == item.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (item.route == Screen.Logout.route) onLogout()
                                else navController.navigateSingleTop(item.route)
                            }
                        )
                        if (index != rest.lastIndex) insetDivider()
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        ) { inner ->
            Box(Modifier.padding(inner)) { content() }
        }
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error) // red
            .height(96.dp)                                // taller header
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_user_placeholder),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(CircleShape)   // larger avatar
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onError,           // white on red
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
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
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun groupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CupertinoCellBackground) // white card
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
private fun insetDivider() {
    Divider(
        color = CupertinoSeparator,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp) // inset under icon
    )
}

@Composable
fun drawerItemRow(
    entry: DrawerItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (entry.enabled) CupertinoLabelPrimary else CupertinoLabelSecondary
    val iconAlpha = if (entry.enabled) 1f else 0.38f
    val label = stringResource(entry.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(22.dp).graphicsLayer(alpha = iconAlpha)
        )
        Spacer(Modifier.width(16.dp))

        // Label
        Text(
            text = label,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        // Optional right count (plain text, not a badge)
        entry.badgeCount?.let {
            Text(
                text = it.toString(),
                color = CupertinoLabelSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.width(8.dp))
        }

        // Chevron for navigable rows
        if (entry.enabled && entry.route != Screen.Logout.route) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = CupertinoLabelSecondary
            )
        }
    }

    if (selected) {
        Divider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        )
    }
}
