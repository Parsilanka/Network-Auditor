package com.securenet.auditor.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.securenet.auditor.SecureNetApp
import com.securenet.auditor.data.db.SpeedTestEntity
import com.securenet.auditor.ui.monitor.DeviceAlert
import com.securenet.auditor.ui.monitor.MonitorViewModel
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.scanner.ScannerViewModel
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary
import com.securenet.auditor.ui.theme.ThemeViewModel
import com.securenet.auditor.ui.tools.RogueApViewModel
import com.securenet.auditor.ui.tools.SpeedTestViewModel
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    scannerViewModel: ScannerViewModel,
    themeViewModel: ThemeViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val container = (context.applicationContext as SecureNetApp).container
    
    val monitorViewModel: MonitorViewModel = viewModel(factory = MonitorViewModel.provideFactory(context))
    val speedTestViewModel: SpeedTestViewModel = viewModel(factory = SpeedTestViewModel.provideFactory(container.speedTestDao))
    val rogueApViewModel: RogueApViewModel = viewModel(factory = RogueApViewModel.provideFactory(context))

    val isMonitorRunning by monitorViewModel.isMonitorRunning.collectAsState()
    val alertHistory by monitorViewModel.alertHistory.collectAsState()
    val speedHistory by speedTestViewModel.history.collectAsState()
    val rogueReport by rogueApViewModel.report.collectAsState()

    var wifiName by remember { mutableStateOf("Checking...") }
    var localIp by remember { mutableStateOf("") }
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            val info = getWifiInfo(context)
            wifiName = info.first
            localIp = info.second
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Network Auditor",
                        fontFamily = MonoType,
                        color = TealPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(imageVector = Icons.Outlined.Menu, contentDescription = "Menu", tint = TealPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { themeViewModel.toggleTheme() }) {
                        Crossfade(targetState = isDarkTheme) { dark ->
                            Icon(
                                imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = TealPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // Wi-Fi Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = if (wifiName == "No Wi-Fi") MaterialTheme.colorScheme.error else SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = wifiName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (wifiName == "No Wi-Fi") "Disconnected" else "Local IP: $localIp",
                            fontFamily = MonoType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Features Group
            Text("Core Security", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            DashboardFeatureCard(
                icon = Icons.Outlined.Wifi,
                title = "Network Scanner",
                subtitle = "Discover devices and open ports",
                onClick = { navController.navigate(Screen.Scanner.route) }
            )
            
            val todayAlerts = alertHistory.count { it.timestamp > System.currentTimeMillis() - 24*60*60*1000 }
            DashboardFeatureCard(
                icon = Icons.Outlined.NotificationsActive,
                title = "Network Monitor",
                statusText = if (isMonitorRunning) "Active" else "Inactive",
                statusColor = if (isMonitorRunning) SuccessGreen else Color.Gray,
                subtitle = if (todayAlerts > 0) "$todayAlerts alerts today" else "No alerts today",
                onClick = { navController.navigate(Screen.Monitor.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Radar,
                title = "Rogue AP Detector",
                statusText = if (rogueReport?.riskLevel == "CRITICAL" || rogueReport?.riskLevel == "HIGH") "Warning ⚠" else "Clean ✓",
                statusColor = if (rogueReport?.riskLevel == "CRITICAL" || rogueReport?.riskLevel == "HIGH") Color.Red else SuccessGreen,
                subtitle = if (rogueReport != null) "Last check: ${rogueReport?.riskLevel}" else "Tap to scan for rogue APs",
                onClick = { navController.navigate(Screen.RogueAp.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Wifi,
                title = "Wi-Fi Scanner",
                subtitle = "Discover and connect to networks",
                onClick = { navController.navigate(Screen.WifiScanner.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.QrCodeScanner,
                title = "QR Wi-Fi Connect",
                subtitle = "Scan QR code to join network",
                onClick = { navController.navigate(Screen.QrScanner.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Tag,
                title = "Hash Generator",
                subtitle = "MD5, SHA-256 hash & verify",
                onClick = { navController.navigate(Screen.HashTool.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Lock,
                title = "Password Auditor",
                subtitle = "Check complexity and resilience",
                onClick = { navController.navigate(Screen.PasswordAuditor.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Analysis & Tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val lastSpeed = speedHistory.firstOrNull()
            DashboardFeatureCard(
                icon = Icons.Outlined.Speed,
                title = "Speed Test",
                subtitle = if (lastSpeed != null) "↓ ${"%.1f".format(lastSpeed.downloadMbps)} Mbps ↑ ${"%.1f".format(lastSpeed.uploadMbps)} Mbps" else "Tap to test network speed",
                onClick = { navController.navigate(Screen.SpeedTest.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Assessment,
                title = "Security Report",
                statusText = "BETA",
                statusColor = TealPrimary,
                subtitle = "Comprehensive vulnerability analysis",
                onClick = { navController.navigate(Screen.VulnerabilityReport.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Search,
                title = "OSINT Intelligence",
                subtitle = "Check breaches and domain info",
                onClick = { navController.navigate(Screen.Osint.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Lock,
                title = "Secure Vault",
                subtitle = "Encrypted scan history",
                onClick = { navController.navigate(Screen.Vault.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.LocationOn,
                title = "IP Geolocation",
                subtitle = "Track IP location & threat level",
                onClick = { navController.navigate(Screen.GeoLocation.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Router,
                title = "SNMP Inspector",
                subtitle = "Query network device metrics",
                onClick = { navController.navigate(Screen.SnmpInspector.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Advanced Enterprise Tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            DashboardFeatureCard(
                icon = Icons.Outlined.Speed,
                title = "Bandwidth Monitor",
                subtitle = "Real-time upload/download graph",
                onClick = { navController.navigate(Screen.Bandwidth.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Lock,
                title = "SSL/TLS Scanner",
                subtitle = "Grade SSL like Qualys SSL Labs",
                onClick = { navController.navigate(Screen.SslScanner.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.Security,
                title = "HTTP Headers Grader",
                subtitle = "Website security header analysis",
                onClick = { navController.navigate(Screen.HeaderGrader.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.AccountTree,
                title = "Subdomain Enumerator",
                subtitle = "DNS reconnaissance & discovery",
                onClick = { navController.navigate(Screen.SubdomainEnum.route) }
            )

            DashboardFeatureCard(
                icon = Icons.Outlined.NetworkCheck,
                title = "Traffic Analyzer",
                subtitle = "Per-app network usage stats",
                onClick = { navController.navigate(Screen.PacketAnalyzer.route) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = {
                    scannerViewModel.startScan()
                    navController.navigate(Screen.Scanner.route)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = TealPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("QUICK SCAN", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DashboardFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusText: String? = null,
    statusColor: Color = TealPrimary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = TealPrimary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (statusText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@SuppressLint("MissingPermission")
fun getWifiInfo(context: Context): Pair<String, String> {
    val connectivityManager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiManager = context.applicationContext.getSystemService(
        Context.WIFI_SERVICE) as WifiManager
    
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager
        .getNetworkCapabilities(network)
    
    val isWifi = capabilities?.hasTransport(
        NetworkCapabilities.TRANSPORT_WIFI) == true
    
    if (!isWifi) {
        return Pair("No Wi-Fi", getLocalIpAddress() ?: "Unknown")
    }
    
    val wifiInfo = wifiManager.connectionInfo
    var ssid = wifiInfo.ssid
    
    // Remove surrounding quotes Android adds
    if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
        ssid = ssid.substring(1, ssid.length - 1)
    }
    
    // Handle unknown SSID (location permission not granted)
    if (ssid == "<unknown ssid>" || ssid.isEmpty()) {
        ssid = "Wi-Fi Connected"
    }
    
    val ip = Formatter.formatIpAddress(wifiInfo.ipAddress)
    return Pair(ssid, ip)
}

fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && 
                    addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (e: Exception) { }
    return null
}
