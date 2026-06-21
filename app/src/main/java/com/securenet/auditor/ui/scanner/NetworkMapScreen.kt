package com.securenet.auditor.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.securenet.auditor.domain.model.ScanProgress
import com.securenet.auditor.ui.components.EmptyStateView
import com.securenet.auditor.ui.components.HostCard
import com.securenet.auditor.ui.theme.MonoType
import com.securenet.auditor.ui.theme.SuccessGreen
import com.securenet.auditor.ui.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMapScreen(viewModel: ScannerViewModel) {
    val progress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val hosts by viewModel.discoveredHosts.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val subnet by viewModel.currentSubnet.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Network Map", fontWeight = FontWeight.Bold)
                        if (subnet != null) {
                            Text(
                                text = "${subnet}.x",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoType,
                                color = TealPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (isScanning) viewModel.stopScan() else viewModel.startScan() },
                containerColor = if (isScanning) MaterialTheme.colorScheme.error else SuccessGreen,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(if (isScanning) "Stop Scan" else "Start Scan") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isScanning) {
                val currentProgress = when (val p = progress) {
                    is ScanProgress.Scanning -> p.current / 254f
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = currentProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = TealPrimary,
                    trackColor = TealPrimary.copy(alpha = 0.1f)
                )
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("${hosts.size} hosts found") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = TealPrimary)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${hosts.sumOf { it.openPorts.size }} ports open") }
                )
            }

            if (hosts.isEmpty() && !isScanning) {
                EmptyStateView(
                    icon = Icons.Outlined.Radar,
                    title = "Network Discovery",
                    subtitle = "Tap scan to discover devices on your network"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(hosts) { host ->
                        HostCard(host = host, onPortScan = { viewModel.runPortScan(it) })
                    }
                    
                    if (progress is ScanProgress.Complete) {
                        item {
                            TextButton(
                                onClick = { /* repository call handled in VM automagically */ },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                enabled = false // Placeholder for visual, VM handles it
                            ) {
                                Text("All hosts saved to vault", color = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}
