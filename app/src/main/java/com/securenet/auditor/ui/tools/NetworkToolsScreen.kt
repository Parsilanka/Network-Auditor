package com.securenet.auditor.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.ui.components.WifiNetworkCard
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkToolsScreen(
    pingViewModel: PingViewModel,
    dnsViewModel: DnsViewModel,
    sslViewModel: SslViewModel,
    wifiViewModel: WifiSecurityViewModel,
    rogueApViewModel: RogueApViewModel,
    wolViewModel: WolViewModel,
    whoisViewModel: WhoisViewModel,
    onBack: () -> Unit,
    onNavigateToSpeedTest: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Ping", "DNS", "SSL", "Wi-Fi", "Rogue AP", "WoL", "Whois", "Speed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            if (title == "Speed") {
                                onNavigateToSpeedTest()
                            } else {
                                selectedTab = index 
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PingTab(pingViewModel)
                1 -> DnsTab(dnsViewModel)
                2 -> SslTab(sslViewModel)
                3 -> WifiTab(wifiViewModel)
                4 -> RogueApTab(rogueApViewModel)
                5 -> WolTab(wolViewModel)
                6 -> WhoisTab(whoisViewModel)
            }
        }
    }
}

@Composable
fun WhoisTab(viewModel: WhoisViewModel) {
    var query by remember { mutableStateOf("") }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("WHOIS Lookup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Lookup registration information for a domain or IP address.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Domain or IP Address") },
            placeholder = { Text("e.g. google.com") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.performWhois(query) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && query.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("LOOKUP WHOIS")
        }

        Spacer(modifier = Modifier.height(24.dp))

        result?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.padding(16.dp).heightIn(max = 300.dp)) {
                    item {
                        Text(it, fontFamily = MonoType, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WolTab(viewModel: WolViewModel) {
    var mac by remember { mutableStateOf("") }
    val status by viewModel.status.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Wake-on-LAN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Wake up a device by sending a Magic Packet to its MAC address.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = mac,
            onValueChange = { mac = it.uppercase() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("MAC Address") },
            placeholder = { Text("00:11:22:33:44:55") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.sendMagicPacket(mac) },
            modifier = Modifier.fillMaxWidth(),
            enabled = mac.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("WAKE UP DEVICE")
        }

        Spacer(modifier = Modifier.height(24.dp))

        status?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (it.startsWith("Error")) MaterialTheme.colorScheme.errorContainer else SuccessGreen.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    fontFamily = MonoType,
                    fontSize = 12.sp,
                    color = if (it.startsWith("Error")) MaterialTheme.colorScheme.error else SuccessGreen
                )
            }
        }
    }
}

@Composable
fun PingTab(viewModel: PingViewModel) {
    var host by remember { mutableStateOf("") }
    val results by viewModel.pingResults.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter IP or domain") },
            singleLine = true,
            enabled = !isRunning
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPingButton("Router", "192.168.1.1", isRunning) { host = it }
            QuickPingButton("Google", "8.8.8.8", isRunning) { host = it }
            QuickPingButton("Cloudflare", "1.1.1.1", isRunning) { host = it }
        }

        Button(
            onClick = { viewModel.startPing(host) },
            modifier = Modifier.fillMaxWidth(),
            enabled = host.isNotBlank() && !isRunning,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("PING")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(results) { result ->
                Text(
                    text = if (result.isSuccess) {
                        "Reply from ${result.host}: time=${result.responseTimeMs}ms"
                    } else {
                        "Request timeout for seq ${result.sequenceNumber}"
                    },
                    fontFamily = MonoType,
                    fontSize = 12.sp,
                    color = if (result.isSuccess) Color(0xFF00BFA5) else Color(0xFFF44336),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        if (results.isNotEmpty() && !isRunning) {
            PingSummaryCard(summary)
        }
    }
}

@Composable
fun DnsTab(viewModel: DnsViewModel) {
    var domain by remember { mutableStateOf("") }
    val records by viewModel.records.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter domain for DNS lookup") },
            placeholder = { Text("e.g. google.com") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.performLookup(domain) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && domain.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("LOOKUP")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(records) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = TealPrimary.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(record.type, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(record.value, fontFamily = MonoType, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SslTab(viewModel: SslViewModel) {
    var domain by remember { mutableStateOf("") }
    val info by viewModel.sslInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter domain for SSL inspection") },
            placeholder = { Text("e.g. google.com") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.inspectCertificate(domain) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && domain.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("INSPECT")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        info?.let { ssl ->
            SslInfoCard(ssl)
        }
    }
}

@Composable
fun SslInfoCard(ssl: SslInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Certificate Details", fontWeight = FontWeight.Bold, color = TealPrimary)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            SslRow("Subject", ssl.subject)
            SslRow("Issuer", ssl.issuer)
            SslRow("Algorithm", ssl.algorithm)
            SslRow("Serial", ssl.serialNumber)
            SslRow("Valid From", ssl.validFrom)
            SslRow("Valid Until", ssl.validUntil)
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (ssl.isExpired) Color.Red.copy(alpha = 0.1f) else SuccessGreen.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    if (ssl.isExpired) "EXPIRED" else "VALID",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (ssl.isExpired) Color.Red else SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SslRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = MonoType)
    }
}

@Composable
fun WifiTab(viewModel: WifiSecurityViewModel) {
    val info by viewModel.wifiInfo.collectAsStateWithLifecycle()
    val availableNetworks by viewModel.availableNetworks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.refreshWifiInfo()
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Connection Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterVertically), strokeWidth = 2.dp, color = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = { viewModel.refreshWifiInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (info == null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("No active Wi-Fi connection detected.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            WifiDetailsCard(info!!)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Available Networks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (availableNetworks.isEmpty()) {
            Text("No other networks detected nearby.", 
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            availableNetworks.forEach { network ->
                WifiNetworkCard(network)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun WifiDetailsCard(info: WifiSecurityInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (info.isSecure) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (info.isSecure) SuccessGreen else Color.Red
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (info.isSecure) "Network is Secure" else "Network Insecure",
                        fontWeight = FontWeight.Bold,
                        color = if (info.isSecure) SuccessGreen else Color.Red
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                WifiStatRow("SSID", info.ssid)
                WifiStatRow("BSSID", info.bssid)
                WifiStatRow("Encryption", info.encryption)
                WifiStatRow("Signal Strength", "${(info.signalLevel + 1) * 20}%")
                WifiStatRow("Frequency", "${info.frequency} MHz")
                WifiStatRow("Link Speed", "${info.linkSpeed} Mbps")
                WifiStatRow("IP Address", info.ipAddress)
                WifiStatRow("Gateway", info.gateway)
                WifiStatRow("DNS 1", info.dns1)
                WifiStatRow("DNS 2", info.dns2)
            }
        }
    }
}

@Composable
fun WifiStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = MonoType)
    }
}
