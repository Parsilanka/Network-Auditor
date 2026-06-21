package com.securenet.auditor.ui.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.scanner.ScannerViewModel
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    scannerViewModel: ScannerViewModel
) {
    val context = LocalContext.current
    var wifiName by remember { mutableStateOf("Checking...") }
    var localIp by remember { mutableStateOf("") }

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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
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

            // Feature Cards
            FeatureCard(
                icon = Icons.Outlined.Wifi,
                title = "Network Scanner",
                subtitle = "Discover devices and open ports",
                onClick = { navController.navigate(Screen.Scanner.route) }
            )
            FeatureCard(
                icon = Icons.Outlined.Search,
                title = "OSINT Intelligence",
                subtitle = "Check breaches and domain info",
                onClick = { navController.navigate(Screen.Osint.route) }
            )
            FeatureCard(
                icon = Icons.Outlined.Lock,
                title = "Secure Vault",
                subtitle = "Encrypted scan history",
                onClick = { navController.navigate(Screen.Vault.route) }
            )

            Spacer(modifier = Modifier.weight(1f))

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
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
