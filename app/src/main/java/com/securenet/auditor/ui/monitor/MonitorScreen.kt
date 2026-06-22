package com.securenet.auditor.ui.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.SubnetScanner
import com.securenet.auditor.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    onBack: () -> Unit
) {
    val isRunning by viewModel.isMonitorRunning.collectAsStateWithLifecycle()
    val deviceCount by viewModel.knownDeviceCount.collectAsStateWithLifecycle()
    val alerts by viewModel.alertHistory.collectAsStateWithLifecycle()
    val whitelist by viewModel.whitelist.collectAsStateWithLifecycle()
    val interval by viewModel.scanInterval.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val subnet = remember { SubnetScanner(context).detectSubnet() ?: "Unknown" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Monitor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonitorControlCard(
                    isRunning = isRunning,
                    onToggle = { if (it) viewModel.startMonitor() else viewModel.stopMonitor() }
                )
            }

            item {
                StatusCard(
                    subnet = subnet,
                    intervalMinutes = (interval / (60 * 1000)).toInt(),
                    deviceCount = deviceCount
                )
            }

            item {
                IntervalSelector(
                    currentIntervalMinutes = (interval / (60 * 1000)).toInt(),
                    onIntervalSelected = { viewModel.setScanInterval(it) }
                )
            }

            if (alerts.isNotEmpty()) {
                item {
                    Text("Recent Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(alerts) { alert ->
                    AlertItem(
                        alert = alert,
                        onAcknowledge = { viewModel.acknowledgeAlert(alert.ipAddress) },
                        onWhitelist = { viewModel.addToWhitelist(alert.ipAddress) }
                    )
                }
            }

            if (whitelist.isNotEmpty()) {
                item {
                    Text("Whitelist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(whitelist.toList()) { ip ->
                    WhitelistItem(
                        ip = ip,
                        onRemove = { viewModel.removeFromWhitelist(ip) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorControlCard(isRunning: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) TealPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isRunning) "Monitor Active" else "Monitor Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) TealPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isRunning) "Scanning for new devices in background" else "Enable to watch for network changes",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = isRunning,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
            )
        }
    }
}

@Composable
fun StatusCard(subnet: String, intervalMinutes: Int, deviceCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Monitoring Status", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Subnet: $subnet.x", style = MaterialTheme.typography.bodyMedium)
            Text("Frequency: Every $intervalMinutes minutes", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = TealPrimary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "$deviceCount Known Devices",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun IntervalSelector(currentIntervalMinutes: Int, onIntervalSelected: (Int) -> Unit) {
    val intervals = listOf(1, 5, 15, 30)
    Column {
        Text("Scan Interval", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            intervals.forEach { min ->
                FilterChip(
                    selected = currentIntervalMinutes == min,
                    onClick = { onIntervalSelected(min) },
                    label = { Text("${min}m") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AlertItem(alert: DeviceAlert, onAcknowledge: () -> Unit, onWhitelist: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.wasAcknowledged) MaterialTheme.colorScheme.surface else Color.Red.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning, 
                    contentDescription = null, 
                    tint = if (alert.wasAcknowledged) Color.Gray else Color.Red,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Device: ${alert.ipAddress}",
                    fontWeight = FontWeight.Bold,
                    color = if (alert.wasAcknowledged) MaterialTheme.colorScheme.onSurface else Color.Red
                )
            }
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(alert.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (!alert.wasAcknowledged) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onWhitelist) {
                        Text("Trust Device")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAcknowledge, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                        Text("Acknowledge")
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistItem(ip: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(ip, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp), tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}
