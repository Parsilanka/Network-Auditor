package com.securenet.auditor.ui.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.RogueApDetector
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary
import com.securenet.auditor.ui.theme.WarningAmber

@Composable
fun RogueApTab(viewModel: RogueApViewModel) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isScanning = scanState is RogueApViewModel.ScanState.Scanning

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = { viewModel.startScan(context) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SCANNING...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Radar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN FOR ROGUE APS", fontWeight = FontWeight.Bold)
                }
            }
        }

        when (val state = scanState) {
            is RogueApViewModel.ScanState.Complete -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CurrentConnectionCard(state.result)
                    }

                    item {
                        val riskLevel = when {
                            state.result.alerts.any { it.threatType == RogueApDetector.RogueThreatType.EVIL_TWIN } -> "CRITICAL"
                            state.result.alerts.isNotEmpty() -> "HIGH"
                            else -> "LOW"
                        }
                        RiskLevelBanner(riskLevel)
                    }

                    if (state.result.alerts.isNotEmpty()) {
                        item {
                            Text("Security Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                        items(state.result.alerts) { alert ->
                            AlertCard(alert)
                        }
                    }

                    item {
                        Text(
                            "All Nearby Networks (${state.result.allNetworks.size})",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    if (state.result.allNetworks.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Text(
                                    "No networks found. This may be due to throttled Wi-Fi scanning (Android limits apps to 4 scans per 2 minutes).",
                                    color = Color(0xFF8B949E),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(state.result.allNetworks) { ap ->
                            NearbyNetworkItem(ap, state.result.currentBssid == ap.bssid)
                        }
                    }
                }
            }
            is RogueApViewModel.ScanState.Error -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D0C0C))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                state.message,
                                color = Color(0xFFFF7B72),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            RogueApViewModel.ScanState.Idle -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Start a scan to detect rogue access points.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RogueApViewModel.ScanState.Scanning -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TealPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Searching for nearby access points...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentConnectionCard(result: RogueApDetector.RogueApScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Network", style = MaterialTheme.typography.labelSmall, color = TealPrimary, fontWeight = FontWeight.Bold)
            Text(result.currentSsid, style = MaterialTheme.typography.titleLarge, fontFamily = MonoType, fontWeight = FontWeight.Bold, color = TealPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("BSSID: ${result.currentBssid}", style = MaterialTheme.typography.bodySmall, fontFamily = MonoType)
        }
    }
}

@Composable
fun RiskLevelBanner(riskLevel: String) {
    val (backgroundColor, textColor, text) = when (riskLevel) {
        "CRITICAL" -> Triple(Color.Red, Color.White, "EVIL TWIN SUSPECTED")
        "HIGH" -> Triple(Color(0xFFE65100), Color.White, "DUPLICATE NETWORKS DETECTED")
        "MEDIUM" -> Triple(WarningAmber, Color.Black, "SUSPICIOUS NETWORKS NEARBY")
        else -> Triple(SuccessGreen, Color.White, "NETWORK APPEARS CLEAN")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "risk")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (riskLevel == "CRITICAL") 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(
                imageVector = if (riskLevel == "LOW") Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AlertCard(alert: RogueApDetector.RogueApAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(alert.targetSsid, fontWeight = FontWeight.Bold, color = Color.Red)
                SignalDots(alert.signalStrength)
            }
            Text("Suspicious BSSID: ${alert.suspiciousBssid}", fontFamily = MonoType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(alert.description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Recommendation: ${alert.recommendation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(8.dp))
            SuggestionChip(
                onClick = {},
                label = { Text(alert.threatType.name, fontSize = 10.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.Red)
            )
        }
    }
}

@Composable
fun NearbyNetworkItem(ap: RogueApDetector.AccessPointInfo, isCurrent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = if (isCurrent) BorderStroke(1.5.dp, Color(0xFF00BFA5)) else BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        ap.ssid,
                        color = Color(0xFFE6EDF3),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF1C3A2E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "CONNECTED",
                                color = Color(0xFF00BFA5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    ap.bssid,
                    color = Color(0xFF8B949E),
                    fontFamily = MonoType,
                    fontSize = 11.sp
                )
                Text(
                    "CH ${ap.channel} • ${if (ap.frequency < 3000) "2.4" else "5.0"} GHz",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${ap.signalStrength} dBm",
                    color = Color(0xFF00BFA5),
                    fontFamily = MonoType,
                    fontSize = 12.sp
                )
                SecurityBadge(ap.capabilities)
            }
        }
    }
}

@Composable
fun SignalDots(level: Int) {
    val dots = when {
        level > -50 -> 5
        level > -60 -> 4
        level > -70 -> 3
        level > -80 -> 2
        else -> 1
    }
    
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { index ->
            Surface(
                modifier = Modifier.size(6.dp),
                shape = CircleShape,
                color = if (index < dots) TealPrimary else Color.Gray.copy(alpha = 0.3f)
            ) {}
        }
    }
}

@Composable
fun SecurityBadge(capabilities: String) {
    val type = when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> "OPEN"
    }
    
    val color = if (type == "OPEN") Color.Red else SuccessGreen
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            type,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
