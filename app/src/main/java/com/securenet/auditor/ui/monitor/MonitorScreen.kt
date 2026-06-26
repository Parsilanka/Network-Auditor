package com.securenet.auditor.ui.monitor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val interval by viewModel.scanInterval.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val subnet = remember { SubnetScanner(context).detectSubnet() ?: "Unknown" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Network Monitor", fontWeight = FontWeight.Bold)
                        Text("24/7 background surveillance", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                MonitorStatusCard(
                    isRunning = isRunning,
                    intervalMinutes = (interval / (60 * 1000)).toInt(),
                    deviceCount = deviceCount,
                    onToggle = { if (it) viewModel.startMonitor() else viewModel.stopMonitor() }
                )
            }

            item {
                MonitoringSettingsCard(
                    currentInterval = (interval / (60 * 1000)).toInt(),
                    onIntervalChange = { viewModel.setScanInterval(it) }
                )
            }

            item {
                KnownDevicesCard(
                    deviceCount = deviceCount,
                    onUpdateBaseline = { viewModel.updateBaseline(emptyList()) },
                    onClearBaseline = { viewModel.clearBaseline() }
                )
            }

            if (alerts.isNotEmpty()) {
                item {
                    AlertHistoryCard(
                        alerts = alerts,
                        onClearHistory = { viewModel.clearAlertHistory() }
                    )
                }
            }
        }
    }
}

@Composable
fun MonitorStatusCard(isRunning: Boolean, intervalMinutes: Int, deviceCount: Int, onToggle: (Boolean) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) TealPrimary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) TealPrimary.copy(alpha = alpha) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isRunning) "MONITORING ACTIVE" else "MONITORING INACTIVE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) TealPrimary else Color.Gray
                    )
                }
                Switch(
                    checked = isRunning,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isRunning) {
                Text("Checking every $intervalMinutes minutes", style = MaterialTheme.typography.bodyMedium)
                Text("Devices in baseline: $deviceCount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Enable to receive alerts for new devices", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun MonitoringSettingsCard(currentInterval: Int, onIntervalChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Monitoring Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Scan interval", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60, 120).forEach { min ->
                    val label = when(min) {
                        60 -> "1 hour"
                        120 -> "2 hours"
                        else -> "$min min"
                    }
                    FilterChip(
                        selected = currentInterval == min,
                        onClick = { onIntervalChange(min) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Alert on new devices", style = MaterialTheme.typography.bodyMedium)
                Checkbox(checked = true, onCheckedChange = {})
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Alert on devices leaving", style = MaterialTheme.typography.bodyMedium)
                Checkbox(checked = false, onCheckedChange = {})
            }
        }
    }
}

@Composable
fun KnownDevicesCard(deviceCount: Int, onUpdateBaseline: () -> Unit, onClearBaseline: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Known Devices", fontWeight = FontWeight.Bold)
                TextButton(onClick = {}) { Text("View All") }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your baseline contains $deviceCount devices.", style = MaterialTheme.typography.bodyMedium)
            
            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUpdateBaseline, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                    Text("Update Baseline", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onClearBaseline, modifier = Modifier.weight(1f)) {
                    Text("Clear Baseline", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AlertHistoryCard(alerts: List<DeviceAlert>, onClearHistory: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Alert History", fontWeight = FontWeight.Bold)
                TextButton(onClick = onClearHistory) { Text("Clear History") }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            alerts.take(5).forEach { alert ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = if (alert.ipAddress.contains("left")) Color.Blue else Color(0xFFFFC107)
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("New device: ${alert.ipAddress}", style = MaterialTheme.typography.bodyMedium)
                        Text(SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(alert.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
