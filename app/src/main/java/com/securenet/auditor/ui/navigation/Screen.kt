package com.securenet.auditor.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Scanner : Screen("scanner")
    object Osint : Screen("osint")
    object Vault : Screen("vault")
    object Ping : Screen("ping")
    object SpeedTest : Screen("speed_test")
    object Monitor : Screen("monitor")
    object VulnerabilityReport : Screen("vuln_report")
    object RogueAp : Screen("rogue_ap")
    object Analytics : Screen("analytics")
}
