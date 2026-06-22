package com.securenet.auditor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.ui.dashboard.DashboardScreen
import com.securenet.auditor.ui.osint.OsintScreen
import com.securenet.auditor.ui.osint.OsintViewModel
import com.securenet.auditor.ui.scanner.NetworkMapScreen
import com.securenet.auditor.ui.scanner.ScannerViewModel
import com.securenet.auditor.ui.theme.ThemeViewModel
import com.securenet.auditor.ui.tools.PingScreen
import com.securenet.auditor.ui.tools.PingViewModel
import com.securenet.auditor.ui.vault.VaultScreen
import com.securenet.auditor.ui.vault.VaultViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as SecureNetApp).container
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.encryptedPrefs))

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.provideFactory(container))
                DashboardScreen(navController, scannerViewModel, themeViewModel)
            }
            composable(Screen.Scanner.route) {
                val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.provideFactory(container))
                NetworkMapScreen(scannerViewModel)
            }
            composable(Screen.Osint.route) {
                val osintViewModel: OsintViewModel = viewModel(factory = OsintViewModel.provideFactory(container))
                OsintScreen(osintViewModel)
            }
            composable(Screen.Vault.route) {
                val vaultViewModel: VaultViewModel = viewModel(factory = VaultViewModel.provideFactory(container))
                VaultScreen(vaultViewModel)
            }
            composable(Screen.Ping.route) {
                val pingViewModel: PingViewModel = viewModel(factory = PingViewModel.provideFactory(container))
                PingScreen(pingViewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
