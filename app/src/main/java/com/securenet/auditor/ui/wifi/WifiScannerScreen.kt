package com.securenet.auditor.ui.wifi

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.network.WifiConnectionManager
import com.securenet.auditor.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScannerScreen(
    viewModel: WifiScannerViewModel,
    onBack: () -> Unit
) {
    val networks by viewModel.networks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentSsid by viewModel.currentSsid.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var selectedNetwork by remember { mutableStateOf<WifiConnectionManager.WifiNetwork?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.scanNetworks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Wi-Fi Networks", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (currentSsid != null) "Connected: $currentSsid" else "Not connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentSsid != null) TealPrimary else Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanNetworks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            ConnectionStatusBanner(connectionState, onDisconnect = { viewModel.disconnect() })

            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { viewModel.scanNetworks() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Networks")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SuggestionChip(
                    onClick = { },
                    label = { Text("${networks.size} networks found") },
                    colors = SuggestionChipDefaults.suggestionChipColors(labelColor = TealPrimary)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SortingOptions()
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(networks) { network ->
                        NetworkCard(
                            network = network,
                            isConnected = network.ssid == currentSsid,
                            onConnect = {
                                if (network.securityType == WifiConnectionManager.WifiSecurityType.OPEN) {
                                    // Show warning for open network
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "⚠ This network has no encryption. Your data may be visible to others.",
                                            actionLabel = "Connect Anyway",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.connectToNetwork(network, null)
                                        }
                                    }
                                } else {
                                    selectedNetwork = network
                                    showPasswordDialog = true
                                }
                            }
                        )
                    }
                    
                    item {
                        NetworkSecurityAnalysis(networks)
                    }
                    
                    item {
                        Android10Notice()
                    }
                }
            }
        }
        
        if (showPasswordDialog && selectedNetwork != null) {
            PasswordDialog(
                network = selectedNetwork!!,
                onDismiss = { showPasswordDialog = false },
                onConnect = { password ->
                    viewModel.connectToNetwork(selectedNetwork!!, password)
                    showPasswordDialog = false
                }
            )
        }
    }
}

@Composable
fun ConnectionStatusBanner(state: WifiScannerViewModel.ConnectionState, onDisconnect: () -> Unit) {
    AnimatedVisibility(
        visible = state !is WifiScannerViewModel.ConnectionState.Idle,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val bgColor: Color
        val contentColor: Color
        val icon: androidx.compose.ui.graphics.vector.ImageVector
        val text: String

        when (state) {
            is WifiScannerViewModel.ConnectionState.Connecting -> {
                bgColor = Color(0xFFFFB300)
                contentColor = Color.Black
                icon = Icons.Default.Sync
                text = "Connecting to ${state.ssid}..."
            }
            is WifiScannerViewModel.ConnectionState.Connected -> {
                bgColor = Color(0xFF4CAF50)
                contentColor = Color.White
                icon = Icons.Default.CheckCircle
                text = "✓ Connected to ${state.ssid}"
            }
            is WifiScannerViewModel.ConnectionState.Failed -> {
                bgColor = Color(0xFFF44336)
                contentColor = Color.White
                icon = Icons.Default.Error
                text = state.message
            }
            else -> {
                bgColor = Color.Gray
                contentColor = Color.White
                icon = Icons.Default.Info
                text = ""
            }
        }

        Surface(
            color = bgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state is WifiScannerViewModel.ConnectionState.Connecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = contentColor, strokeWidth = 2.dp)
                } else {
                    Icon(icon, contentDescription = null, tint = contentColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text, color = contentColor, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                
                if (state is WifiScannerViewModel.ConnectionState.Connected) {
                    TextButton(onClick = onDisconnect) {
                        Text("Disconnect", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkCard(
    network: WifiConnectionManager.WifiNetwork,
    isConnected: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isConnected) 2.dp else 0.dp,
                color = if (isConnected) TealPrimary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val signalIcon = when(network.signalLevel) {
                        4 -> Icons.Default.Wifi
                        3 -> Icons.Default.Wifi
                        2 -> Icons.Default.Wifi
                        1 -> Icons.Default.Wifi
                        else -> Icons.Default.WifiOff
                    }
                    val signalColor = when(network.signalLevel) {
                        4 -> Color(0xFF4CAF50)
                        3 -> TealPrimary
                        2 -> Color(0xFFFFB300)
                        1 -> Color(0xFFF44336)
                        else -> Color.Gray
                    }
                    Icon(signalIcon, contentDescription = null, tint = signalColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(network.ssid, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                SecurityChip(network.securityType)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val signalBlocks = "█".repeat(network.signalLevel) + "░".repeat(5 - network.signalLevel)
                    Text("Signal: $signalBlocks  ${network.signalStrength} dBm", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("BSSID: ${network.bssid}", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val freq = if (network.frequency > 4900) "5.0 GHz" else "2.4 GHz"
                    Text("$freq Ch ${network.channel}", fontSize = 12.sp)
                    if (isConnected) {
                        Surface(
                            color = TealPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("✓ Connected", color = TealPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onConnect,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Connect", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityChip(type: WifiConnectionManager.WifiSecurityType) {
    val (color, label) = when (type) {
        WifiConnectionManager.WifiSecurityType.OPEN -> Color(0xFF4CAF50) to "OPEN"
        WifiConnectionManager.WifiSecurityType.WPA3 -> TealPrimary to "WPA3"
        WifiConnectionManager.WifiSecurityType.WPA2 -> Color(0xFF2196F3) to "WPA2"
        WifiConnectionManager.WifiSecurityType.WPA -> Color(0xFFFFB300) to "WPA"
        WifiConnectionManager.WifiSecurityType.WEP -> Color(0xFFF44336) to "WEP ⚠"
        else -> Color.Gray to "UNKNOWN"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SortingOptions() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = true, onClick = {}, label = { Text("Signal") })
        FilterChip(selected = false, onClick = {}, label = { Text("Security") })
        FilterChip(selected = false, onClick = {}, label = { Text("Name") })
        FilterChip(selected = false, onClick = {}, label = { Text("Freq") })
    }
}

@Composable
fun PasswordDialog(
    network: WifiConnectionManager.WifiNetwork,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to ${network.ssid}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SecurityChip(network.securityType)
                    Spacer(modifier = Modifier.width(8.dp))
                    val signalBlocks = "█".repeat(network.signalLevel) + "░".repeat(5 - network.signalLevel)
                    Text(signalBlocks, fontFamily = FontFamily.Monospace)
                }
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Wi-Fi Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    }
                )
                
                if (network.securityType == WifiConnectionManager.WifiSecurityType.WEP) {
                    Text("⚠ WEP encryption is outdated and insecure", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(password) },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NetworkSecurityAnalysis(networks: List<WifiConnectionManager.WifiNetwork>) {
    val openCount = networks.count { it.securityType == WifiConnectionManager.WifiSecurityType.OPEN }
    val wepCount = networks.count { it.securityType == WifiConnectionManager.WifiSecurityType.WEP }
    val secureCount = networks.count { it.securityType in listOf(WifiConnectionManager.WifiSecurityType.WPA2, WifiConnectionManager.WifiSecurityType.WPA3, WifiConnectionManager.WifiSecurityType.WPA2_WPA3) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Network Security Overview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total networks found: ${networks.size}")
            Text("Open networks: $openCount", color = if (openCount > 0) Color.Red else MaterialTheme.colorScheme.onSurface)
            Text("WEP networks: $wepCount", color = if (wepCount > 0) Color.Red else MaterialTheme.colorScheme.onSurface)
            Text("WPA2/WPA3: $secureCount", color = Color(0xFF4CAF50))
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            if (openCount > 0) {
                Text("⚠ $openCount open network(s) detected nearby. Avoid connecting to open networks in public areas.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
            if (wepCount > 0) {
                Text("⚠ $wepCount WEP network(s) detected. WEP is broken encryption. Do not use these networks.", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun Android10Notice() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Android 10+ Connection Notice", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "On Android 10 and above, a system dialog will appear to confirm the connection. This is required by Android for security. Tap 'Connect' in the system dialog to complete the connection.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
