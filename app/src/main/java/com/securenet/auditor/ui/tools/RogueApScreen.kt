package com.securenet.auditor.ui.tools

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
    val report by viewModel.report.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Button(
            onClick = { viewModel.startScan() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("SCANNING...")
            } else {
                Icon(Icons.Default.Radar, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN FOR ROGUE APS")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            report?.let { r ->
                item {
                    CurrentConnectionCard(r)
                }

                item {
                    RiskLevelBanner(r.riskLevel)
                }

                if (r.suspiciousNetworks.isNotEmpty()) {
                    item {
                        Text("Suspicious Networks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                    items(r.suspiciousNetworks) { ap ->
                        SuspiciousApCard(ap)
                    }
                }

                item {
                    Text("All Nearby Networks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                items(r.nearbyNetworks.sortedByDescending { it.signalStrength }) { ap ->
                    NearbyNetworkItem(ap)
                }
            } ?: item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Start a scan to detect rogue access points.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun CurrentConnectionCard(report: RogueApDetector.RogueApReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Network", style = MaterialTheme.typography.labelSmall, color = TealPrimary, fontWeight = FontWeight.Bold)
            Text(report.connectedSsid ?: "Not Connected", style = MaterialTheme.typography.titleLarge, fontFamily = MonoType, fontWeight = FontWeight.Bold, color = TealPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("BSSID: ${report.connectedBssid ?: "N/A"}", style = MaterialTheme.typography.bodySmall, fontFamily = MonoType)
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
fun SuspiciousApCard(ap: RogueApDetector.ApScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ap.ssid, fontWeight = FontWeight.Bold, color = Color.Red)
                SignalDots(ap.signalStrength)
            }
            Text(ap.bssid, fontFamily = MonoType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 4.dp, crossAxisSpacing = 4.dp) {
                ap.suspicionReasons.forEach { reason ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(reason, fontSize = 10.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.Red)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing)
    ) {
        content()
    }
}

@Composable
fun NearbyNetworkItem(ap: RogueApDetector.ApScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ap.ssid, fontWeight = FontWeight.Medium)
                Text(ap.bssid, style = MaterialTheme.typography.labelSmall, fontFamily = MonoType, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                SignalDots(ap.signalStrength)
                Spacer(modifier = Modifier.height(4.dp))
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
