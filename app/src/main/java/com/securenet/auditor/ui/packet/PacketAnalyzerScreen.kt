package com.securenet.auditor.ui.packet

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.securenet.auditor.network.PacketAnalyzer
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketAnalyzerScreen(
    navController: NavController,
    viewModel: PacketAnalyzerViewModel,
    onBack: () -> Unit
) {
    val appUsage by viewModel.appUsage.collectAsStateWithLifecycle()
    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var timeRange by remember { mutableStateOf(24) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.loadStats(timeRange)
        }
        viewModel.startConnectionMonitoring()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Traffic Analyzer", fontWeight = FontWeight.Bold)
                        Text("Network usage & connections", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { /* Range selector logic */ }) {
                        Text("${timeRange}h", color = TealPrimary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFF161B22), contentColor = TealPrimary) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("App Usage") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Active") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Timeline") })
            }

            when (selectedTab) {
                0 -> AppUsageTab(appUsage, isLoading)
                1 -> ActiveConnectionsTab(activeConnections, navController)
                2 -> TimelineTab()
            }
        }
    }
}

@Composable
fun AppUsageTab(usage: List<PacketAnalyzer.NetworkSession>, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TotalSummaryCard(usage)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(usage.take(10)) { session ->
                    UsageBarRow(session, usage.firstOrNull()?.let { it.rxBytes + it.txBytes } ?: 1L)
                }
            }
        }
    }
}

@Composable
fun TotalSummaryCard(usage: List<PacketAnalyzer.NetworkSession>) {
    val totalRx = usage.sumOf { it.rxBytes }
    val totalTx = usage.sumOf { it.txBytes }
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total traffic today", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("↓ ${formatBytes(totalRx)}", fontWeight = FontWeight.Bold, color = TealPrimary, fontSize = 20.sp)
                Text("↑ ${formatBytes(totalTx)}", fontWeight = FontWeight.Bold, color = Color(0xFF0288D1), fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun UsageBarRow(session: PacketAnalyzer.NetworkSession, maxBytes: Long) {
    val total = session.rxBytes + session.txBytes
    val progress = (total.toFloat() / maxBytes).coerceIn(0f, 1f)
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.appName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(formatBytes(total), fontSize = 12.sp, fontFamily = MonoType)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 4.dp),
            color = TealPrimary,
            trackColor = Color.Gray.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ActiveConnectionsTab(connections: List<PacketAnalyzer.ConnectionInfo>, navController: NavController) {
    var filter by remember { mutableStateOf("ALL") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "ESTABLISHED", "LISTEN", "OTHER").forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val filtered = if (filter == "ALL") connections else connections.filter { 
                if (filter == "OTHER") it.state !in listOf("ESTABLISHED", "LISTEN") else it.state == filter 
            }
            items(filtered) { conn ->
                ConnectionRow(conn, navController)
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: PacketAnalyzer.ConnectionInfo, navController: NavController) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${conn.localAddress} → ${conn.remoteAddress}", fontFamily = MonoType, fontSize = 12.sp)
                val color = when(conn.state) {
                    "ESTABLISHED" -> Color(0xFF4CAF50)
                    "LISTEN" -> TealPrimary
                    "TIME_WAIT" -> Color(0xFFFFC107)
                    "CLOSE_WAIT" -> Color.Red
                    else -> Color.Gray
                }
                Text(conn.state, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { 
                val remoteIp = conn.remoteAddress.substringBefore(":")
                navController.navigate(Screen.GeoLocation.route + "?ip=$remoteIp")
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Geolocate", tint = TealPrimary)
            }
        }
    }
}

@Composable
fun TimelineTab() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Network Usage Timeline (Last 24h)", color = Color.Gray)
        // Placeholder for stacked bar chart
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1048576 -> "${"%.1f".format(bytes/1024f)} KB"
        bytes < 1073741824 -> "${"%.1f".format(bytes/1048576f)} MB"
        else -> "${"%.2f".format(bytes/1073741824f)} GB"
    }
}
