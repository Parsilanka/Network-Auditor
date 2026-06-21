package com.securenet.auditor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.securenet.auditor.ui.theme.TealPrimary

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Screen.Dashboard to (Icons.Outlined.Home to "Dashboard"),
        Screen.Scanner to (Icons.Outlined.Wifi to "Scanner"),
        Screen.Osint to (Icons.Outlined.Search to "OSINT"),
        Screen.Vault to (Icons.Outlined.Lock to "Vault")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { (screen, iconData) ->
            NavigationBarItem(
                icon = { Icon(iconData.first, contentDescription = iconData.second) },
                label = { Text(iconData.second) },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = TealPrimary.copy(alpha = 0.2f),
                    selectedIconColor = TealPrimary,
                    selectedTextColor = TealPrimary
                )
            )
        }
    }
}
