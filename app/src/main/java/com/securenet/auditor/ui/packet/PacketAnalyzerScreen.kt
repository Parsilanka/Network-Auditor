package com.securenet.auditor.ui.packet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.securenet.auditor.network.PacketAnalyzer
import com.securenet.auditor.ui.navigation.Screen
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.TealPrimary
import kotlinx.coroutines.delay
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketAnalyzerScreen(
    navController: NavController,
    viewModel: PacketAnalyzerViewModel,
    onBack: () -> Unit
) {
    val appUsage by viewModel.appUsage.collectAsStateWithLifecycle()
    val activeConnections by viewModel.activeConnections.collectAsStateWithLifecycle()
    val hourlyTraffic by viewModel.hourlyTraffic.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var timeRange by remember { mutableStateOf(24) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.loadStats(timeRange)
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModel.loadHourlyTraffic(context)
        }
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
                1 -> ActiveConnectionsTab(activeConnections, navController, viewModel)
                2 -> TimelineTab(hourlyTraffic)
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
            val maxBytes = usage.maxOfOrNull { it.rxBytes + it.txBytes } ?: 1L
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(usage.take(10)) { session ->
                    UsageBarRow(session, maxBytes)
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
    val downloadFraction = (session.rxBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    val uploadFraction = (session.txBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
    
    val isSystem = session.appName == "Unknown"
    val displayName = if (isSystem) "System & Other" else session.appName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon Placeholder
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = if (isSystem) Color(0xFF30363D) else TealPrimary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isSystem) {
                    Icon(
                        Icons.Outlined.Android,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // App name column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName,
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (isSystem) {
                    Text(
                        "Kernel, VPN, and unattributed system traffic",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        formatBytes(total),
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Download bar
                LinearProgressIndicator(
                    progress = downloadFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00BFA5),
                    trackColor = Color(0xFF21262D)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Upload bar
                LinearProgressIndicator(
                    progress = uploadFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF0288D1),
                    trackColor = Color(0xFF21262D)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Size column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "↓ ${formatBytes(session.rxBytes)}",
                    color = Color(0xFF00BFA5),
                    fontSize = 11.sp,
                    fontFamily = MonoType
                )
                Text(
                    "↑ ${formatBytes(session.txBytes)}",
                    color = Color(0xFF0288D1),
                    fontSize = 11.sp,
                    fontFamily = MonoType
                )
            }
        }
    }
}

@Composable
fun ActiveConnectionsTab(connections: List<PacketAnalyzer.ConnectionInfo>, navController: NavController, viewModel: PacketAnalyzerViewModel) {
    var filter by remember { mutableStateOf("ALL") }
    val context = LocalContext.current
    var lastUpdated by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var secondsAgo by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadActiveConnections(context)
            lastUpdated = System.currentTimeMillis()
            delay(3000) // Refresh every 3 seconds
        }
    }

    LaunchedEffect(lastUpdated) {
        while (true) {
            secondsAgo = ((System.currentTimeMillis() - lastUpdated) / 1000).toInt()
            delay(1000)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "ESTABLISHED", "LISTEN", "ACTIVE", "OTHER").forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Text(
            "Last updated: $secondsAgo seconds ago",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8B949E),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val filtered = if (filter == "ALL") connections else connections.filter { 
            if (filter == "OTHER") it.state !in listOf("ESTABLISHED", "LISTEN", "ACTIVE") else it.state == filter 
        }

        if (filtered.isEmpty() || (filtered.size == 1 && filtered.first().remoteAddress == "Active Interface")) {
            // Android 10+ Restricted State or No Connections
            
            // NETWORK INTERFACES CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    
                    Row(verticalAlignment = 
                        Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.NetworkCheck,
                            contentDescription = null,
                            tint = Color(0xFF00BFA5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Active Network Interfaces",
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Show all network interfaces
                    val interfaces = remember {
                        NetworkInterface.getNetworkInterfaces()
                            ?.asSequence()
                            ?.filter { it.isUp }
                            ?.toList() ?: emptyList()
                    }
                    
                    interfaces.forEach { iface ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = 
                                Arrangement.SpaceBetween,
                            verticalAlignment = 
                                Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    iface.displayName,
                                    color = Color(0xFF00BFA5),
                                    fontFamily = MonoType,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                iface.inetAddresses.asSequence()
                                    .filter { !it.isLoopbackAddress }
                                    .forEach { addr ->
                                    Text(
                                        addr.hostAddress ?: "",
                                        color = Color(0xFF8B949E),
                                        fontFamily = MonoType,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            // Status chip
                            Surface(
                                color = if (iface.isUp)
                                    Color(0xFF0D2D1A)
                                else Color(0xFF3D0C0C),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (iface.isUp) "UP" else "DOWN",
                                    color = if (iface.isUp)
                                        Color(0xFF56D364)
                                    else Color(0xFFFF7B72),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            color = Color(0xFF30363D),
                            modifier = Modifier
                                .padding(vertical = 4.dp))
                    }
                }
            }

            // DNS SERVERS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF161B22))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = 
                        Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = Color(0xFF00BFA5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DNS Configuration",
                            color = Color(0xFFE6EDF3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Read DNS from system properties
                    val dnsServers = remember {
                        mutableListOf<String>().apply {
                            try {
                                val cm = context.getSystemService(
                                    Context.CONNECTIVITY_SERVICE) 
                                    as ConnectivityManager
                                val network = cm.activeNetwork
                                val props = cm
                                    .getLinkProperties(network)
                                props?.dnsServers?.forEach { dns ->
                                    add(dns.hostAddress ?: "")
                                }
                            } catch (e: Exception) {}
                        }
                    }
                    
                    if (dnsServers.isEmpty()) {
                        Text(
                            "DNS servers not accessible",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp
                        )
                    } else {
                        dnsServers.forEachIndexed { i, dns ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = 
                                    Arrangement.SpaceBetween,
                                verticalAlignment = 
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    "DNS ${i + 1}",
                                    color = Color(0xFF8B949E),
                                    fontSize = 12.sp
                                )
                                Row(verticalAlignment = 
                                    Alignment.CenterVertically) {
                                    Text(
                                        dns,
                                        color = Color(0xFF00BFA5),
                                        fontFamily = MonoType,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = 
                                        Modifier.width(8.dp))
                                    // Copy button
                                    IconButton(
                                        onClick = {
                                            val clipboard = context
                                                .getSystemService(
                                                Context
                                                .CLIPBOARD_SERVICE)
                                                as ClipboardManager
                                            clipboard.setPrimaryClip(
                                                ClipData.newPlainText(
                                                    "DNS", dns))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = Color(0xFF8B949E),
                                            modifier = Modifier
                                                .size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                filtered.forEach { conn ->
                    ConnectionRow(conn, navController)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: PacketAnalyzer.ConnectionInfo, navController: NavController) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${conn.localAddress} → ${conn.remoteAddress}",
                    color = Color(0xFFE6EDF3),
                    fontFamily = MonoType,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when (conn.state) {
                        "ESTABLISHED" -> Color(0xFF0D2D1A)
                        "LISTEN" -> Color(0xFF0C2D4A)
                        "ACTIVE" -> Color(0xFF1C3A2E)
                        "TIME_WAIT" -> Color(0xFF3D2A00)
                        else -> Color(0xFF21262D)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        conn.state,
                        color = when (conn.state) {
                            "ESTABLISHED" -> Color(0xFF56D364)
                            "LISTEN" -> Color(0xFF79C0FF)
                            "ACTIVE" -> Color(0xFF00BFA5)
                            "TIME_WAIT" -> Color(0xFFF0B429)
                            else -> Color(0xFF8B949E)
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            IconButton(
                onClick = { 
                    val remoteIp = conn.remoteAddress.substringBefore(":")
                    navController.navigate(Screen.GeoLocation.route + "?ip=$remoteIp")
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = "Geolocate",
                    tint = Color(0xFF00BFA5),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineTab(
    hourlyData: List<PacketAnalyzer.HourlyTraffic>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Network Usage Timeline (Last 24h)",
            color = Color(0xFFE6EDF3),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (hourlyData.isEmpty() || 
            hourlyData.all { it.downloadMb == 0f && it.uploadMb == 0f }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No timeline data available yet",
                        color = Color(0xFF8B949E)
                    )
                }
            }
            return
        }

        val maxValue = hourlyData.maxOf { it.downloadMb + it.uploadMb }.coerceAtLeast(1f)

        // Stacked bar chart using Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color(0xFF161B22), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val barWidth = size.width / 24f * 0.7f
            val spacing = size.width / 24f * 0.3f
            
            hourlyData.forEachIndexed { hour, data ->
                val x = hour * (barWidth + spacing) + spacing / 2
                
                val downloadHeight = (data.downloadMb / maxValue) * size.height
                val uploadHeight = (data.uploadMb / maxValue) * size.height
                
                // Download bar (teal, bottom)
                drawRect(
                    color = Color(0xFF00BFA5),
                    topLeft = Offset(x, size.height - downloadHeight),
                    size = Size(barWidth, downloadHeight)
                )
                
                // Upload bar (blue, stacked on top)
                drawRect(
                    color = Color(0xFF0288D1),
                    topLeft = Offset(x, size.height - downloadHeight - uploadHeight),
                    size = Size(barWidth, uploadHeight)
                )
            }
            
            // Horizontal grid lines
            for (i in 0..3) {
                val y = size.height * (i / 3f)
                drawLine(
                    color = Color(0xFF30363D),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X axis hour labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "06:00", "12:00", "18:00", "23:00").forEach { label ->
                Text(
                    label,
                    color = Color(0xFF8B949E),
                    fontSize = 10.sp,
                    fontFamily = MonoType
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF00BFA5), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Download", 
                color = Color(0xFF8B949E),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color(0xFF0288D1), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "Upload", 
                color = Color(0xFF8B949E),
                fontSize = 12.sp
            )
        }
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
