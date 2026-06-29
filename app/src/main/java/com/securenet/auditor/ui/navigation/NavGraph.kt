package com.securenet.auditor.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.ui.dashboard.DashboardScreen
import com.securenet.auditor.ui.geolocation.GeoLocationScreen
import com.securenet.auditor.ui.geolocation.GeoLocationViewModel
import com.securenet.auditor.ui.monitor.MonitorScreen
import com.securenet.auditor.ui.monitor.MonitorViewModel
import com.securenet.auditor.ui.osint.OsintScreen
import com.securenet.auditor.ui.osint.OsintViewModel
import com.securenet.auditor.ui.report.VulnerabilityReportScreen
import com.securenet.auditor.ui.report.VulnerabilityViewModel
import com.securenet.auditor.ui.scanner.NetworkMapScreen
import com.securenet.auditor.ui.scanner.ScannerViewModel
import com.securenet.auditor.ui.snmp.SnmpScreen
import com.securenet.auditor.ui.snmp.SnmpViewModel
import com.securenet.auditor.ui.theme.ThemeViewModel
import com.securenet.auditor.ui.tools.*
import com.securenet.auditor.ui.vault.VaultScreen
import com.securenet.auditor.ui.vault.VaultViewModel
import com.securenet.auditor.ui.wifi.WifiScannerScreen
import com.securenet.auditor.ui.wifi.WifiScannerViewModel
import com.securenet.auditor.ui.qrscanner.QrScannerScreen
import com.securenet.auditor.ui.qrscanner.QrScannerViewModel
import com.securenet.auditor.ui.bandwidth.*
import com.securenet.auditor.ui.ssl.*
import com.securenet.auditor.ui.headers.*
import com.securenet.auditor.ui.subdomain.*
import com.securenet.auditor.ui.packet.*
import com.securenet.auditor.ui.portknocker.PortKnockerScreen
import com.securenet.auditor.ui.portknocker.PortKnockerViewModel
import com.securenet.auditor.ui.dnsleak.DnsLeakScreen
import com.securenet.auditor.ui.dnsleak.DnsLeakViewModel
import com.securenet.auditor.ui.ssh.SshTerminalScreen
import com.securenet.auditor.ui.ssh.SshTerminalViewModel
import com.securenet.auditor.ui.rogueap.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as SecureNetApp).container
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.encryptedPrefs))
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    navController = navController,
                    onItemClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
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
                    DashboardScreen(
                        navController = navController,
                        scannerViewModel = scannerViewModel,
                        themeViewModel = themeViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Screen.Scanner.route) {
                    val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.provideFactory(container))
                    NetworkMapScreen(navController, scannerViewModel)
                }
                composable(Screen.GeoLocation.route + "?ip={ip}") { backStackEntry ->
                    val ip = backStackEntry.arguments?.getString("ip")
                    val geoViewModel: GeoLocationViewModel = viewModel(factory = GeoLocationViewModel.factory(container.geoLocationRepository))
                    GeoLocationScreen(navController, geoViewModel, initialIp = ip)
                }
                composable(Screen.SnmpInspector.route + "?ip={ip}") { backStackEntry ->
                    val ip = backStackEntry.arguments?.getString("ip")
                    val snmpViewModel: SnmpViewModel = viewModel(factory = SnmpViewModel.factory(container.snmpClient))
                    SnmpScreen(navController, snmpViewModel, initialIp = ip)
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
                    val wolViewModel: WolViewModel = viewModel(factory = WolViewModel.provideFactory())
                    val whoisViewModel: WhoisViewModel = viewModel(factory = WhoisViewModel.provideFactory(container.osintRepository))
                    NetworkToolsScreen(
                        pingViewModel = pingViewModel,
                        dnsViewModel = dnsViewModel,
                        sslViewModel = sslViewModel,
                        wifiViewModel = wifiViewModel,
                        rogueApViewModel = rogueApViewModel,
                        wolViewModel = wolViewModel,
                        whoisViewModel = whoisViewModel,
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
                composable(Screen.WifiScanner.route) {
                    val wifiViewModel: WifiScannerViewModel = viewModel(factory = WifiScannerViewModel.provideFactory(container))
                    WifiScannerScreen(
                        viewModel = wifiViewModel,
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
                composable(Screen.HashTool.route) {
                    val hashViewModel: HashViewModel = viewModel(factory = HashViewModel.provideFactory())
                    HashScreen(
                        viewModel = hashViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.PasswordAuditor.route) {
                    val passwordViewModel: PasswordViewModel = viewModel(factory = PasswordViewModel.provideFactory(container))
                    PasswordAuditorScreen(navController, passwordViewModel)
                }
                composable(Screen.PasswordManager.route) {
                    val passwordViewModel: PasswordViewModel = viewModel(factory = PasswordViewModel.provideFactory(container))
                    PasswordManagerScreen(navController, passwordViewModel)
                }
                composable(Screen.SubnetCalculator.route) {
                    val subnetViewModel: SubnetCalculatorViewModel = viewModel(factory = SubnetCalculatorViewModel.provideFactory())
                    SubnetCalculatorScreen(
                        viewModel = subnetViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Wol.route) {
                    val wolViewModel: WolViewModel = viewModel(factory = WolViewModel.provideFactory())
                    WolScreen(
                        viewModel = wolViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.QrScanner.route) {
                    val qrViewModel: QrScannerViewModel = viewModel(factory = QrScannerViewModel.provideFactory(container))
                    QrScannerScreen(
                        navController = navController,
                        viewModel = qrViewModel
                    )
                }
                composable(Screen.Bandwidth.route) {
                    val bandwidthViewModel: BandwidthViewModel = viewModel(factory = BandwidthViewModel.provideFactory(container.bandwidthMonitor))
                    BandwidthScreen(bandwidthViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.SslScanner.route) {
                    val sslViewModel: SslScannerViewModel = viewModel(factory = SslScannerViewModel.provideFactory(container.sslTlsScanner))
                    SslScannerScreen(sslViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.HeaderGrader.route) {
                    val headerViewModel: HeaderGraderViewModel = viewModel(factory = HeaderGraderViewModel.provideFactory(container.httpHeaderAnalyzer))
                    HeaderGraderScreen(headerViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.SubdomainEnum.route) {
                    val subdomainViewModel: SubdomainViewModel = viewModel(factory = SubdomainViewModel.provideFactory(container.subdomainEnumerator))
                    SubdomainScreen(navController, subdomainViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.PacketAnalyzer.route) {
                    val packetViewModel: PacketAnalyzerViewModel = viewModel(factory = PacketAnalyzerViewModel.provideFactory(container.packetAnalyzer))
                    PacketAnalyzerScreen(navController, packetViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.PortKnocker.route) {
                    val portKnockerViewModel: PortKnockerViewModel = viewModel(factory = PortKnockerViewModel.provideFactory())
                    PortKnockerScreen(portKnockerViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.DnsLeak.route) {
                    val dnsLeakViewModel: DnsLeakViewModel = viewModel(factory = DnsLeakViewModel.provideFactory())
                    DnsLeakScreen(dnsLeakViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.SshTerminal.route) {
                    val sshViewModel: SshTerminalViewModel = viewModel(factory = SshTerminalViewModel.provideFactory(container))
                    SshTerminalScreen(sshViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.RogueAp.route) {
                    val rogueApViewModel: RogueApViewModel = viewModel(factory = RogueApViewModel.provideFactory(context))
                    
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Rogue AP Detector", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
}

@Composable
fun DrawerContent(
    navController: NavController,
    onItemClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Network Auditor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        Text("CORE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp))

        DrawerItem(
            icon = Icons.Outlined.Dashboard,
            label = "Dashboard",
            selected = currentRoute == Screen.Dashboard.route,
            onClick = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Wifi,
            label = "Network Scanner",
            selected = currentRoute == Screen.Scanner.route,
            onClick = {
                navController.navigate(Screen.Scanner.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Assessment,
            label = "Security Report",
            selected = currentRoute == Screen.VulnerabilityReport.route,
            onClick = {
                navController.navigate(Screen.VulnerabilityReport.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.NotificationsActive,
            label = "Network Monitor",
            selected = currentRoute == Screen.Monitor.route,
            onClick = {
                navController.navigate(Screen.Monitor.route)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("TOOLS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
        
        DrawerItem(
            icon = Icons.Outlined.Build,
            label = "Network Tools",
            selected = currentRoute == Screen.Ping.route,
            onClick = {
                navController.navigate(Screen.Ping.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.QrCodeScanner,
            label = "QR Scanner",
            selected = currentRoute == Screen.QrScanner.route,
            onClick = {
                navController.navigate(Screen.QrScanner.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Tag,
            label = "Hash Tool",
            selected = currentRoute == Screen.HashTool.route,
            onClick = {
                navController.navigate(Screen.HashTool.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Lock,
            label = "Password Auditor",
            selected = currentRoute == Screen.PasswordAuditor.route,
            onClick = {
                navController.navigate(Screen.PasswordAuditor.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Password,
            label = "Password Manager",
            selected = currentRoute == Screen.PasswordManager.route,
            onClick = {
                navController.navigate(Screen.PasswordManager.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Calculate,
            label = "Subnet Calculator",
            selected = currentRoute == Screen.SubnetCalculator.route,
            onClick = {
                navController.navigate(Screen.SubnetCalculator.route)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("INTELLIGENCE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))

        DrawerItem(
            icon = Icons.Outlined.Search,
            label = "OSINT Intelligence",
            selected = currentRoute == Screen.Osint.route,
            onClick = {
                navController.navigate(Screen.Osint.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Contactless,
            label = "Breach Checker",
            selected = currentRoute == Screen.Osint.route, // Shares route for now
            onClick = {
                navController.navigate(Screen.Osint.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.LocationOn,
            label = "IP Geolocation",
            selected = currentRoute == Screen.GeoLocation.route,
            onClick = {
                navController.navigate(Screen.GeoLocation.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Router,
            label = "SNMP Inspector",
            selected = currentRoute == Screen.SnmpInspector.route,
            onClick = {
                navController.navigate(Screen.SnmpInspector.route)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("STORAGE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))

        DrawerItem(
            icon = Icons.Outlined.Lock,
            label = "Secure Vault",
            selected = currentRoute == Screen.Vault.route,
            onClick = {
                navController.navigate(Screen.Vault.route)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("ENTERPRISE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))

        DrawerItem(
            icon = Icons.Outlined.Speed,
            label = "Bandwidth Monitor",
            selected = currentRoute == Screen.Bandwidth.route,
            onClick = {
                navController.navigate(Screen.Bandwidth.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Lock,
            label = "SSL Scanner",
            selected = currentRoute == Screen.SslScanner.route,
            onClick = {
                navController.navigate(Screen.SslScanner.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Security,
            label = "Header Grader",
            selected = currentRoute == Screen.HeaderGrader.route,
            onClick = {
                navController.navigate(Screen.HeaderGrader.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.AccountTree,
            label = "Subdomain Enum",
            selected = currentRoute == Screen.SubdomainEnum.route,
            onClick = {
                navController.navigate(Screen.SubdomainEnum.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.NetworkCheck,
            label = "Traffic Analyzer",
            selected = currentRoute == Screen.PacketAnalyzer.route,
            onClick = {
                navController.navigate(Screen.PacketAnalyzer.route)
                onItemClick()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("ADVANCED", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))

        DrawerItem(
            icon = Icons.Outlined.DoorBack,
            label = "Port Knocker",
            selected = currentRoute == Screen.PortKnocker.route,
            onClick = {
                navController.navigate(Screen.PortKnocker.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Dns,
            label = "DNS Leak Test",
            selected = currentRoute == Screen.DnsLeak.route,
            onClick = {
                navController.navigate(Screen.DnsLeak.route)
                onItemClick()
            }
        )
        DrawerItem(
            icon = Icons.Outlined.Terminal,
            label = "SSH Terminal",
            selected = currentRoute == Screen.SshTerminal.route,
            onClick = {
                navController.navigate(Screen.SshTerminal.route)
                onItemClick()
            }
        )
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
