package com.securenet.auditor.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Scanner : Screen("scanner")
    object Osint : Screen("osint")
    object Vault : Screen("vault")
    object Ping : Screen("ping")
}
