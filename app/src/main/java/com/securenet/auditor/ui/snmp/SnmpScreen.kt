package com.securenet.auditor.ui.snmp

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.navigation.NavController
import com.securenet.auditor.domain.model.SnmpDeviceInfo
import com.securenet.auditor.domain.model.SnmpDeviceType
import com.securenet.auditor.network.snmp.SnmpClient
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnmpScreen(
    navController: NavController,
    viewModel: SnmpViewModel,
    initialIp: String? = null
) {
    var ipInput by remember { mutableStateOf(initialIp ?: "") }
    var communityInput by remember { mutableStateOf("public") }
    var showCommunity by remember { mutableStateOf(false) }
    val scanState by viewModel.deviceInfo.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scannedDevices by viewModel.scannedDevices.collectAsStateWithLifecycle()
    val progressText by viewModel.progress.collectAsStateWithLifecycle()
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(initialIp) {
        if (!initialIp.isNullOrBlank()) {
            viewModel.scanDevice(initialIp, communityInput)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SNMP Inspector", fontWeight = FontWeight.Bold)
                        Text("Network Device Monitor", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Section
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target IP Address") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = MonoType),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = communityInput,
                        onValueChange = { communityInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Community String") },
                        trailingIcon = {
                            IconButton(onClick = { showCommunity = !showCommunity }) {
                                Icon(
                                    if (showCommunity) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (showCommunity) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("public", "private", "cisco", "community")) { community ->
                            AssistChip(
                                onClick = { communityInput = community },
                                label = { Text(community) }
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.scanDevice(ipInput, communityInput) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning && ipInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Scan Device")
                    }

                    if (isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TealPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(progressText, style = MaterialTheme.typography.labelSmall, color = TealPrimary)
                        }
                    }
                }
            }

            if (scannedDevices.isNotEmpty() && !isScanning && scanState is SnmpScanState.Idle) {
                Text("Recent Devices", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scannedDevices.take(5)) { device ->
                        InputChip(
                            selected = false,
                            onClick = {
                                ipInput = device.ipAddress
                                communityInput = device.community
                                viewModel.scanDevice(device.ipAddress, device.community)
                            },
                            label = { Text(device.ipAddress, fontFamily = MonoType) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getDeviceIcon(device.deviceType),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Results Section
            when (val state = scanState) {
                is SnmpScanState.Success -> {
                    SnmpResultContent(state.data, onRescan = { viewModel.scanDevice(ipInput, communityInput) })
                }
                is SnmpScanState.Error -> {
                    ErrorCard(state.message)
                }
                is SnmpScanState.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) { SnmpShimmerCard() }
                    }
                }
                else -> {}
            }
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("About SNMP") },
                text = {
                    Text("SNMP (Simple Network Management Protocol) allows querying network devices for performance data. Most routers and managed switches support SNMP v1/v2c. Default community string is usually 'public'.")
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) { Text("Got it") }
                }
            )
        }
    }
}

@Composable
fun SnmpShimmerCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22).copy(alpha = alpha))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color.Gray.copy(alpha = 0.3f), CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Box(modifier = Modifier.width(100.dp).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(150.dp).height(10.dp).background(Color.Gray.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
fun SnmpResultContent(device: SnmpDeviceInfo, onRescan: () -> Unit) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Device Header Card
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getDeviceIcon(device.deviceType),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = TealPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(device.deviceType.name, color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(device.systemName ?: "Unknown Device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (!device.systemLocation.isNullOrBlank()) {
                    Text("Location: ${device.systemLocation}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (!device.systemContact.isNullOrBlank()) {
                    Text("Contact: ${device.systemContact}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(device.ipAddress, fontFamily = MonoType, color = TealPrimary, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↑ Online for ${device.uptime ?: "Unknown"}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(color = Color.Gray.copy(alpha = 0.2f), shape = MaterialTheme.shapes.extraSmall) {
                        Text("${device.scanTimeMs}ms", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                }
            }
        }

        // System Description
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("System Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    device.systemDescription ?: "No description available",
                    fontFamily = MonoType,
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        // Performance Metrics
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Performance Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                if (device.memoryUsagePercent != null) {
                    MetricProgress("Memory Usage", device.memoryUsagePercent / 100f) {
                        val usedMb = (device.totalMemoryKb!! - device.freeMemoryKb!!) / 1024
                        val totalMb = device.totalMemoryKb / 1024
                        Text("${usedMb}MB used of ${totalMb}MB (${device.memoryUsagePercent}%)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (device.storageUsagePercent != null) {
                    MetricProgress("Storage Usage", device.storageUsagePercent / 100f) {
                        Text("${device.storageUsagePercent}% of capacity used", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (device.cpuLoad1Min != null) {
                    Column {
                        Text("CPU Load (1 min avg)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        val load = device.cpuLoad1Min.toDoubleOrNull() ?: 0.0
                        val color = when {
                            load < 1.0 -> Color(0xFF4CAF50)
                            load < 2.0 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                        val statusText = when {
                            load < 1.0 -> "Low"
                            load < 2.0 -> "Moderate"
                            else -> "High"
                        }
                        Text("${device.cpuLoad1Min} ($statusText)", color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Network Activity
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Network Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${device.interfaceCount ?: "0"} network interfaces", style = MaterialTheme.typography.bodySmall)
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    TrafficItem(Modifier.weight(1f), "↓ Inbound", device.inboundTraffic, Color(0xFF4CAF50))
                    TrafficItem(Modifier.weight(1f), "↑ Outbound", device.outboundTraffic, TealPrimary)
                }
                
                Text("${device.tcpConnections ?: "0"} active TCP connections", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Security Assessment
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Security Assessment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                
                val findings = mutableListOf<SecurityFinding>()
                if (device.community == "public") {
                    findings.add(SecurityFinding("Default 'public' community in use", "Change to a unique community string", Severity.MEDIUM))
                }
                if (device.community == "private") {
                    findings.add(SecurityFinding("Write community 'private' detected", "Disable write access if not needed", Severity.HIGH))
                }
                val tcpCount = device.tcpConnections?.toIntOrNull() ?: 0
                if (tcpCount > 100) {
                    findings.add(SecurityFinding("High number of TCP connections ($tcpCount)", "Monitor for potential DDoS or saturation", Severity.LOW))
                }
                if (device.memoryUsagePercent != null && device.memoryUsagePercent > 90) {
                    findings.add(SecurityFinding("Critical memory usage: ${device.memoryUsagePercent}%", "Investigate memory-intensive processes", Severity.HIGH))
                }
                if (device.storageUsagePercent != null && device.storageUsagePercent > 85) {
                    findings.add(SecurityFinding("High storage usage: ${device.storageUsagePercent}%", "Clean up logs or expand capacity", Severity.MEDIUM))
                }

                if (findings.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No security issues detected", color = Color(0xFF4CAF50))
                    }
                } else {
                    findings.forEach { finding ->
                        SecurityFindingItem(finding)
                    }
                }
            }
        }

        // Raw OID Data
        var rawExpanded by remember { mutableStateOf(false) }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { rawExpanded = !rawExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Raw OID Values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Icon(if (rawExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
                AnimatedVisibility(visible = rawExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RawOidRow("sysDescr", SnmpClient.OID_SYSTEM_DESCR, device.systemDescription)
                        RawOidRow("sysName", SnmpClient.OID_SYSTEM_NAME, device.systemName)
                        RawOidRow("sysUptime", SnmpClient.OID_SYSTEM_UPTIME, device.uptime)
                        RawOidRow("ifInOctets", SnmpClient.OID_IF_IN_OCTETS, device.inboundTraffic)
                        RawOidRow("ifOutOctets", SnmpClient.OID_IF_OUT_OCTETS, device.outboundTraffic)
                        // Add more as needed
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) { Text("Re-Scan") }
            OutlinedButton(
                onClick = {
                    val report = "SNMP Device Report\nIP: ${device.ipAddress}\nType: ${device.deviceType}\nUptime: ${device.uptime}\n"
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, report)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Report"))
                },
                modifier = Modifier.weight(1f)
            ) { Text("Share") }
        }
    }
}

@Composable
fun MetricProgress(label: String, progress: Float, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        val color = when {
            progress < 0.6f -> Color(0xFF4CAF50)
            progress < 0.8f -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
        val animatedProgress by animateFloatAsState(targetValue = progress)
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        content()
    }
}

@Composable
fun TrafficItem(modifier: Modifier, label: String, value: String?, color: Color) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(formatOctets(value), color = color, fontFamily = MonoType, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecurityFindingItem(finding: SecurityFinding) {
    val color = when (finding.severity) {
        Severity.LOW -> Color(0xFF2196F3)
        Severity.MEDIUM -> Color(0xFFFFC107)
        Severity.HIGH, Severity.CRITICAL -> Color(0xFFF44336)
    }
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
            Text(
                finding.severity.name,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                fontSize = 8.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(finding.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(finding.recommendation, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun RawOidRow(name: String, oid: String, value: String?) {
    Column {
        Text("$name ($oid)", fontSize = 10.sp, color = TealPrimary)
        Text(value ?: "N/A", fontFamily = MonoType, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Troubleshooting:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            val tips = listOf(
                "Ensure the device has SNMP enabled",
                "Check the community string is correct",
                "Verify UDP port 161 is not blocked",
                "Some devices only support SNMP v2c or v3",
                "Try community strings: public, private, cisco"
            )
            tips.forEach { tip ->
                Text("• $tip", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

private fun getDeviceIcon(type: SnmpDeviceType) = when (type) {
    SnmpDeviceType.ROUTER -> Icons.Outlined.Router
    SnmpDeviceType.SWITCH -> Icons.Outlined.DeviceHub
    SnmpDeviceType.SERVER -> Icons.Outlined.Storage
    SnmpDeviceType.LINUX_HOST, SnmpDeviceType.WINDOWS_HOST -> Icons.Outlined.Computer
    SnmpDeviceType.PRINTER -> Icons.Outlined.Print
    SnmpDeviceType.UNKNOWN -> Icons.Outlined.Devices
}

private fun formatOctets(value: String?): String {
    val octets = value?.toLongOrNull() ?: return "0 B"
    return when {
        octets < 1024 -> "$octets B"
        octets < 1048576 -> "${octets / 1024} KB"
        octets < 1073741824 -> "${octets / 1048576} MB"
        else -> "${octets / 1073741824} GB"
    }
}

data class SecurityFinding(val title: String, val recommendation: String, val severity: Severity)
enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
