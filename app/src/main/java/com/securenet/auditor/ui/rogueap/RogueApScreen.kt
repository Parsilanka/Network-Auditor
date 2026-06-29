package com.securenet.auditor.ui.rogueap

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.RogueApDetector
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import com.securenet.auditor.ui.tools.RogueApViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RogueApScreen(
    viewModel: RogueApViewModel,
    onBack: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var continuousMonitoring by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Rogue AP Detector", fontWeight = FontWeight.Bold)
                        Text("Evil twin attack detection", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Button(
                onClick = { viewModel.startScan() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = !isScanning
            ) {
                if (isScanning) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Scan for Rogue APs")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current Network Status (Simplified)
            Text("Current Network Status", fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Your trusted network", fontWeight = FontWeight.Bold, color = TealPrimary)
                        Text("Protected via WPA2/WPA3", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isScanning && alerts.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("No Rogue Access Points Detected", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Text("Your network appears clean. No evil twin attacks were detected.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(alerts) { alert ->
                        RogueApAlertCard(alert)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable continuous monitoring", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = continuousMonitoring, onCheckedChange = { continuousMonitoring = it })
            }
        }
    }
}

@Composable
fun RogueApAlertCard(alert: RogueApDetector.RogueApAlert) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(alert.threatType.name.replace("_", " "), fontWeight = FontWeight.Bold, color = Color.Red)
                }
                Surface(color = Color.Red, shape = MaterialTheme.shapes.small) {
                    Text("CRITICAL", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Target SSID: ${alert.targetSsid}", fontSize = 14.sp)
            Text("Suspicious BSSID: ${alert.suspiciousBssid}", fontFamily = MonoType, fontSize = 12.sp, color = Color.Gray)
            Text("Signal: ${alert.signalStrength} dBm (very strong)", fontSize = 12.sp)
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Red.copy(alpha = 0.2f))
            Text(alert.description, style = MaterialTheme.typography.bodySmall)
            Text(alert.recommendation, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Red)
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { /* Report logic */ }) { Text("Report") }
                Button(onClick = { /* Disconnect logic */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Disconnect Immediately")
                }
            }
        }
    }
}
