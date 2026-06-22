package com.securenet.auditor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.ui.dashboard.DashboardScreen
import com.securenet.auditor.ui.monitor.MonitorScreen
import com.securenet.auditor.ui.monitor.MonitorViewModel
import com.securenet.auditor.ui.osint.OsintScreen
import com.securenet.auditor.ui.osint.OsintViewModel
import com.securenet.auditor.ui.report.VulnerabilityReportScreen
import com.securenet.auditor.ui.report.VulnerabilityViewModel
import com.securenet.auditor.ui.scanner.NetworkMapScreen
import com.securenet.auditor.ui.scanner.ScannerViewModel
import com.securenet.auditor.ui.theme.ThemeViewModel
import com.securenet.auditor.ui.tools.*
import com.securenet.auditor.ui.vault.VaultScreen
import com.securenet.auditor.ui.vault.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
                val dnsViewModel: DnsViewModel = viewModel(factory = DnsViewModel.provideFactory())
                val sslViewModel: SslViewModel = viewModel(factory = SslViewModel.provideFactory())
                val wifiViewModel: WifiSecurityViewModel = viewModel(factory = WifiSecurityViewModel.provideFactory(context))
                val rogueApViewModel: RogueApViewModel = viewModel(factory = RogueApViewModel.provideFactory(context))
                NetworkToolsScreen(
                    pingViewModel = pingViewModel,
                    dnsViewModel = dnsViewModel,
                    sslViewModel = sslViewModel,
                    wifiViewModel = wifiViewModel,
                    rogueApViewModel = rogueApViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSpeedTest = { navController.navigate(Screen.SpeedTest.route) }
                )
            }
            composable(Screen.SpeedTest.route) {
                val speedTestViewModel: SpeedTestViewModel = viewModel(factory = SpeedTestViewModel.provideFactory(container.speedTestDao))
                SpeedTestScreen(
                    viewModel = speedTestViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Monitor.route) {
                val monitorViewModel: MonitorViewModel = viewModel(factory = MonitorViewModel.provideFactory(context))
                MonitorScreen(
                    viewModel = monitorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.VulnerabilityReport.route) {
                val vulnerabilityViewModel: VulnerabilityViewModel = viewModel(factory = VulnerabilityViewModel.provideFactory(container))
                VulnerabilityReportScreen(
                    viewModel = vulnerabilityViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RogueAp.route) {
                val rogueApViewModel: RogueApViewModel = viewModel(factory = RogueApViewModel.provideFactory(context))
                
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Rogue AP Detector", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
                        RogueApTab(rogueApViewModel)
                    }
                }
            }
        }
    }
}
